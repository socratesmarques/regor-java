package com.regor.backend;
import com.regor.backend.dto.Api.*;
import com.regor.backend.entity.SwipeKind;
import com.regor.backend.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.springframework.mock.web.MockMultipartFile;

/** Servidor temporário com dados criados pelas regras reais, somente para testes visuais. */
public final class UiFixtureServer extends PostgresTestBase {
    public static void main(String[] args) throws Exception {
        Path stop=Path.of("target","stop-ui-fixture");Files.deleteIfExists(stop);
        try(var app=SpringApplication.run(RegorBackendApplication.class,
            "--server.port=0","--spring.datasource.url="+POSTGRES.getJdbcUrl("postgres","postgres"),
            "--spring.datasource.username=postgres","--spring.datasource.password=postgres",
            "--regor.jwt.secret=ui-test-only-secret-at-least-32-bytes")) {
            var auth=app.getBean(AuthService.class);var profiles=app.getBean(ProfileService.class);
            var matching=app.getBean(MatchingService.class);var chats=app.getBean(ChatService.class);
            Session alice=auth.register(new Register("alice@regor.test","fixture-password","Alice","alice",LocalDate.of(1997,5,10)));
            Session bruno=auth.register(new Register("bruno@regor.test","fixture-password","Bruno","bruno",LocalDate.of(1995,2,12)));
            Session carla=auth.register(new Register("carla@regor.test","fixture-password","Carla","carla",LocalDate.of(1998,8,22)));
            int index=0;
            for(Session session:java.util.List.of(alice,bruno,carla)) {
                String name=java.util.List.of("Alice","Bruno","Carla").get(index);
                profiles.update(session.user().id(),new ProfileEdit(name,name.toLowerCase(),LocalDate.of(1995+index,5,10),
                    "São Paulo, SP","Gosto de boas conversas, viagens e de descobrir novos lugares.","Música, Cinema, Natureza","",-23.55,-46.63));
                var image=new BufferedImage(600,480,BufferedImage.TYPE_INT_RGB);
                var graphics=image.createGraphics();
                graphics.setPaint(new GradientPaint(0,0,new Color(55+index*35,25,85),600,480,new Color(215,50+index*20,115)));
                graphics.fillRect(0,0,600,480);graphics.setColor(new Color(245,220,230));
                graphics.fillOval(235,100,130,130);graphics.fillRoundRect(160,240,280,270,140,140);graphics.dispose();
                var output=new ByteArrayOutputStream();ImageIO.write(image,"png",output);
                profiles.upload(session.user().id(),new MockMultipartFile("file","fixture.png","image/png",output.toByteArray()));
                index++;
            }
            matching.swipe(alice.user().id(),new SwipeRequest(bruno.user().id(),SwipeKind.LIKE));
            var match=matching.swipe(bruno.user().id(),new SwipeRequest(alice.user().id(),SwipeKind.SUPERLIKE));
            chats.send(bruno.user().id(),match.chatId(),new SendMessage(UUID.randomUUID(),"Oi, Alice! Qual foi o último lugar que você descobriu?"));
            int port=((ServletWebServerApplicationContext)app).getWebServer().getPort();
            new ObjectMapper().writeValue(Path.of("target","ui-fixture.json").toFile(),Map.of("port",port,"chatId",match.chatId().toString()));
            System.out.println("REGOR_UI_FIXTURE_READY port="+port);
            while(!Files.exists(stop))Thread.sleep(500);
        } finally { POSTGRES.close(); }
    }
}
