package com.regor.desktop.service;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class ApiClientTest {
    @Test void sendsBearerAndJsonAndPreservesServerErrors() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var authorization=new AtomicReference<String>();var payload=new AtomicReference<String>();
        server.createContext("/api/swipes",exchange->{
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            payload.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] response="{\"matchId\":null}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,response.length);exchange.getResponseBody().write(response);exchange.close();
        });
        server.createContext("/api/denied",exchange->{
            byte[] response="{\"message\":\"Somente participantes\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(403,response.length);exchange.getResponseBody().write(response);exchange.close();
        });
        server.start();
        try(var api=new ApiClient("http://127.0.0.1:"+server.getAddress().getPort())) {
            api.authenticate(ApiClient.JSON.readTree("{\"token\":\"test-token\",\"user\":{\"id\":\"user\"}}"));
            api.post("/api/swipes",Map.of("targetId","other","kind","LIKE"));
            assertEquals("Bearer test-token",authorization.get());
            assertEquals("LIKE",ApiClient.JSON.readTree(payload.get()).get("kind").asText());
            var error=assertThrows(ApiClient.ApiException.class,()->api.get("/api/denied"));
            assertEquals(403,error.status);assertEquals("Somente participantes",error.getMessage());
            api.logout();assertFalse(api.authenticated());
        }finally{server.stop(0);}
    }
    @Test void requiresHttpsOutsideLoopback() {
        assertThrows(IllegalArgumentException.class,()->new ApiClient("http://example.com"));
    }
}
