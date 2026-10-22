package com.regor.backend.websocket;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regor.backend.service.RealtimeEvent;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.event.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.*;

@Component
public class RealtimeHandler extends TextWebSocketHandler {
    private record Connection(UUID user,Instant expires,String token,WebSocketSession session) {}
    private final Map<String,Connection> connections=new ConcurrentHashMap<>();
    private final ObjectMapper json;
    private final com.regor.backend.service.PresenceService presence;
    private final org.springframework.security.oauth2.jwt.JwtDecoder decoder;
    public RealtimeHandler(ObjectMapper json, com.regor.backend.service.PresenceService presence, org.springframework.security.oauth2.jwt.JwtDecoder decoder) { this.json=json;this.presence=presence;this.decoder=decoder; }
    @Override public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if(!(session.getPrincipal() instanceof JwtAuthenticationToken auth)) { session.close(CloseStatus.POLICY_VIOLATION);return; }
        connections.put(session.getId(),new Connection(UUID.fromString(auth.getToken().getSubject()),
            auth.getToken().getExpiresAt(),auth.getToken().getTokenValue(),new ConcurrentWebSocketSessionDecorator(session,5000,65536)));
        presence.connected(session.getId(),java.util.UUID.fromString(auth.getToken().getSubject()));
    }
    @Override protected void handleTextMessage(WebSocketSession session,TextMessage message) throws Exception {
        // Writes use the REST transaction; no client-supplied subscription can select another user's events.
        if(!message.getPayload().equals("ping")) session.close(CloseStatus.POLICY_VIOLATION);
    }
    @Override public void afterConnectionClosed(WebSocketSession session,CloseStatus status) { connections.remove(session.getId());presence.disconnected(session.getId()); }
    @Override public void handleTransportError(WebSocketSession session,Throwable error) throws Exception {
        connections.remove(session.getId());presence.disconnected(session.getId());session.close(CloseStatus.SERVER_ERROR);
    }
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void committed(RealtimeEvent event) {
        for(var c:connections.values()) if(c.user().equals(event.userId())) {
            try {
                if(!valid(c)) { c.session().close(CloseStatus.POLICY_VIOLATION);continue; }
                c.session().sendMessage(new TextMessage(json.writeValueAsString(Map.of("type",event.type(),"data",event.data()))));
            } catch(Exception e) {
                connections.remove(c.session().getId());presence.disconnected(c.session().getId());
                try { c.session().close(CloseStatus.SERVER_ERROR); } catch(Exception ignored) {}
            }
        }
    }
    private boolean valid(Connection connection) {
        try {decoder.decode(connection.token());return true;}catch(org.springframework.security.oauth2.jwt.JwtException e){return false;}
    }
    @Scheduled(fixedDelay=30000) public void expireSessions() {
        for(var c:connections.values()) if(!valid(c)) {
            connections.remove(c.session().getId());presence.disconnected(c.session().getId());
            try { c.session().close(CloseStatus.POLICY_VIOLATION); } catch(Exception ignored) {}
        }
    }
}
