package com.regor.backend;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.regor.backend.dto.Api.*;
import com.regor.backend.entity.SwipeKind;
import com.regor.backend.service.MatchingService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.net.http.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT) @AutoConfigureMockMvc
class SocialIntegrationTest extends PostgresTestBase {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc;
    @Autowired MatchingService matching; @LocalServerPort int port;
    record Person(UUID id,String token) {}
    @BeforeEach void clean() { jdbc.execute("TRUNCATE users CASCADE"); }
    Person person(String name) throws Exception {
        String body=json.writeValueAsString(Map.of("email",name+"@example.com","password","long-password",
            "displayName",name,"username",name,"birthDate","1995-01-01"));
        var r=mvc.perform(post("/api/auth/register").contentType("application/json").content(body))
            .andExpect(status().isCreated()).andReturn();
        var n=json.readTree(r.getResponse().getContentAsString());return new Person(UUID.fromString(n.at("/user/id").asText()),n.get("token").asText());
    }
    JsonNode getJson(Person p,String path) throws Exception {
        return json.readTree(mvc.perform(get(URI.create(path)).header("Authorization","Bearer "+p.token()))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    JsonNode swipe(Person a,Person b,String kind) throws Exception {
        return json.readTree(mvc.perform(post("/api/swipes").header("Authorization","Bearer "+a.token())
            .contentType("application/json").content(json.writeValueAsString(Map.of("targetId",b.id(),"kind",kind))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    UUID match(Person a,Person b) throws Exception { swipe(a,b,"LIKE");return UUID.fromString(swipe(b,a,"LIKE").get("chatId").asText()); }
    @Test void persistAllSwipeKindsAndCreateOnlyReciprocalMatch() throws Exception {
        var a=person("alice");var b=person("bruno");var c=person("carla");
        assertThat(swipe(a,b,"LIKE").get("matchId").isNull()).isTrue();
        swipe(a,c,"DISLIKE");
        assertThat(jdbc.queryForObject("select count(*) from matches",Integer.class)).isZero();
        var result=swipe(b,a,"SUPERLIKE");
        assertThat(result.get("matchId").isNull()).isFalse();
        swipe(b,a,"SUPERLIKE");
        assertThat(jdbc.queryForObject("select count(*) from matches",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from chats",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from chat_participants",Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForList("select kind from swipes",String.class)).containsExactlyInAnyOrder("LIKE","DISLIKE","SUPERLIKE");
        assertThat(getJson(a,"/api/matches")).hasSize(1);
        assertThat(getJson(a,"/api/discovery")).isEmpty();
    }
    @Test void simultaneousReciprocalLikesCreateOneMatchAndChat() throws Exception {
        var a=person("alice");var b=person("bruno");
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            var start=new CountDownLatch(1);
            var one=executor.submit(()->{start.await();return matching.swipe(a.id(),new SwipeRequest(b.id(),SwipeKind.LIKE));});
            var two=executor.submit(()->{start.await();return matching.swipe(b.id(),new SwipeRequest(a.id(),SwipeKind.LIKE));});
            start.countDown();one.get(15,TimeUnit.SECONDS);two.get(15,TimeUnit.SECONDS);
        }
        assertThat(jdbc.queryForObject("select count(*) from matches",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from chats",Integer.class)).isEqualTo(1);
    }
    @Test void rejectSelfSwipeAndForgedActor() throws Exception {
        var a=person("alice");var b=person("bruno");
        mvc.perform(post("/api/swipes").header("Authorization","Bearer "+a.token()).contentType("application/json")
            .content(json.writeValueAsString(Map.of("targetId",a.id(),"kind","LIKE")))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/swipes").header("Authorization","Bearer "+a.token()).contentType("application/json")
            .content(json.writeValueAsString(Map.of("targetId",b.id(),"actorId",b.id(),"kind","LIKE")))).andExpect(status().isBadRequest());
    }
    @Test void chatRequiresMatchAndMembershipAndMessagesAreIdempotent() throws Exception {
        var a=person("alice");var b=person("bruno");var c=person("carla");
        var unknown=UUID.randomUUID();
        mvc.perform(get("/api/chats/"+unknown+"/messages").header("Authorization","Bearer "+a.token())).andExpect(status().isForbidden());
        UUID chat=match(a,b);
        mvc.perform(get("/api/chats/"+chat+"/messages").header("Authorization","Bearer "+c.token())).andExpect(status().isForbidden());
        String body=json.writeValueAsString(Map.of("clientId",UUID.randomUUID(),"content","Olá!"));
        mvc.perform(post("/api/chats/"+chat+"/messages").header("Authorization","Bearer "+c.token())
            .contentType("application/json").content(body)).andExpect(status().isForbidden());
        for(int i=0;i<2;i++) mvc.perform(post("/api/chats/"+chat+"/messages").header("Authorization","Bearer "+a.token())
            .contentType("application/json").content(body)).andExpect(status().isOk());
        assertThat(getJson(b,"/api/chats/"+chat+"/messages")).hasSize(1);
        assertThat(jdbc.queryForObject("select content from messages",String.class)).isEqualTo("Olá!");
    }
    @Test void profileAndPreferencesRejectMassAssignment() throws Exception {
        var a=person("alice");
        ObjectNode edit=json.createObjectNode().put("displayName","Alice").put("username","alice")
            .put("birthDate","1995-01-01").put("location","São Paulo").put("bio","Olá").put("interests","Música").put("gender","Mulher");
        for(String key:List.of("role","vip","isVip","verified","coins","permissions")) {
            var bad=edit.deepCopy();bad.put(key,"ADMIN");
            mvc.perform(put("/api/profiles/me").header("Authorization","Bearer "+a.token()).contentType("application/json")
                .content(json.writeValueAsString(bad))).andExpect(status().isBadRequest());
        }
        mvc.perform(put("/api/profiles/me").header("Authorization","Bearer "+a.token()).contentType("application/json")
            .content(json.writeValueAsString(edit))).andExpect(status().isOk());
        var prefs=(ObjectNode)getJson(a,"/api/preferences");prefs.put("coins",10000);
        mvc.perform(put("/api/preferences").header("Authorization","Bearer "+a.token()).contentType("application/json")
            .content(json.writeValueAsString(prefs))).andExpect(status().isBadRequest());
        assertThat(getJson(a,"/api/users/me").get("role").asText()).isEqualTo("USER");
        assertThat(jdbc.queryForObject("select coins from users",Long.class)).isZero();
    }
    @Test void serverSideDiscoveryFiltersAndCoordinatePrivacy() throws Exception {
        var a=person("alice");var b=person("bruno");var c=person("carla");
        jdbc.update("update profiles set location='Recife',interests='Música',latitude=-8.05,longitude=-34.9 where id=?",b.id());
        jdbc.update("update profiles set latitude=-8.06,longitude=-34.91 where id=?",a.id());
        jdbc.update("update profiles set location='São Paulo',interests='Cinema',latitude=-23.55,longitude=-46.63 where id=?",c.id());
        assertThat(getJson(a,"/api/discovery?location=Recife&interest=M%C3%BAsica")).hasSize(1);
        assertThat(getJson(a,"/api/discovery?distance=20")).hasSize(1);
        assertThat(getJson(a,"/api/discovery?maxAge=20")).isEmpty();
        assertThat(getJson(a,"/api/profiles/"+b.id()).get("latitude").isNull()).isTrue();
        jdbc.update("update user_preferences set discoverable=false where id=?",b.id());
        assertThat(getJson(a,"/api/discovery?distance=20")).isEmpty();
    }
    @Test void photoContentValidatedAndDeletionOwned() throws Exception {
        var a=person("alice");var b=person("bruno");
        var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(4,4,BufferedImage.TYPE_INT_RGB),"png",bytes);
        var result=mvc.perform(multipart("/api/photos").file(new MockMultipartFile("file","a.png","image/png",bytes.toByteArray()))
            .header("Authorization","Bearer "+a.token())).andExpect(status().isCreated()).andReturn();
        String photo=json.readTree(result.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(get("/api/photos/"+photo).header("Authorization","Bearer "+b.token())).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(delete("/api/photos/"+photo).header("Authorization","Bearer "+b.token())).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/photos").file(new MockMultipartFile("file","bad.png","image/png","not image".getBytes()))
            .header("Authorization","Bearer "+a.token())).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/photos/"+photo).header("Authorization","Bearer "+a.token())).andExpect(status().isNoContent());
    }
    @Test void notificationsCanOnlyBeMarkedByOwner() throws Exception {
        var a=person("alice");var b=person("bruno");swipe(a,b,"LIKE");
        String id=getJson(b,"/api/notifications").get(0).get("id").asText();
        mvc.perform(put("/api/notifications/"+id+"/read").header("Authorization","Bearer "+a.token())).andExpect(status().isForbidden());
        mvc.perform(put("/api/notifications/"+id+"/read").header("Authorization","Bearer "+b.token())).andExpect(status().isNoContent());
        assertThat(getJson(b,"/api/notifications").get(0).get("readAt").isNull()).isFalse();
    }
    static class Inbox implements WebSocket.Listener {
        final BlockingQueue<String> messages=new LinkedBlockingQueue<>();
        final StringBuilder partial=new StringBuilder();
        @Override public void onOpen(WebSocket socket) { socket.request(1); }
        @Override public CompletionStage<?> onText(WebSocket socket,CharSequence text,boolean last) {
            partial.append(text);if(last) {messages.add(partial.toString());partial.setLength(0);}socket.request(1);return null;
        }
    }
    @Test void websocketDeliversCommittedMessagesOnlyToParticipants() throws Exception {
        var a=person("alice");var b=person("bruno");var c=person("carla");UUID chat=match(a,b);
        try(var client=HttpClient.newHttpClient()) {
            var received=new Inbox();var outsider=new Inbox();
            var ws=client.newWebSocketBuilder().header("Authorization","Bearer "+b.token())
                .buildAsync(URI.create("ws://localhost:"+port+"/ws"),received).get(10,TimeUnit.SECONDS);
            var other=client.newWebSocketBuilder().header("Authorization","Bearer "+c.token())
                .buildAsync(URI.create("ws://localhost:"+port+"/ws"),outsider).get(10,TimeUnit.SECONDS);
            try {
                mvc.perform(post("/api/chats/"+chat+"/messages").header("Authorization","Bearer "+a.token()).contentType("application/json")
                    .content(json.writeValueAsString(Map.of("clientId",UUID.randomUUID(),"content","Tempo real")))).andExpect(status().isOk());
                String event=received.messages.poll(5,TimeUnit.SECONDS);
                assertThat(event).contains("\"type\":\"message\"","Tempo real");
                assertThat(outsider.messages.poll(500,TimeUnit.MILLISECONDS)).isNull();
                assertThat(getJson(b,"/api/chats/"+chat+"/messages")).hasSize(1);
            } finally { ws.abort();other.abort(); }
            assertThatThrownBy(()->client.newWebSocketBuilder().buildAsync(URI.create("ws://localhost:"+port+"/ws"),new Inbox()).join())
                .hasCauseInstanceOf(WebSocketHandshakeException.class);
        }
    }
}
