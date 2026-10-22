package com.regor.backend.config;
import com.regor.backend.websocket.RealtimeHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.*;

@Configuration @EnableWebSocket @EnableScheduling
public class WebSocketConfig implements WebSocketConfigurer {
    private final RealtimeHandler handler;
    public WebSocketConfig(RealtimeHandler handler) { this.handler=handler; }
    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler,"/ws");
    }
}
