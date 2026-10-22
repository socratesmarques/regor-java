package com.regor.desktop.service;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

final class RealtimeClient implements AutoCloseable {
    private final HttpClient http;private final URI base;
    private final ScheduledExecutorService scheduler=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"regor-reconnect");t.setDaemon(true);return t;});
    private final AtomicInteger epoch=new AtomicInteger();
    private volatile WebSocket socket;
    RealtimeClient(HttpClient http,URI base) { this.http=http;this.base=base; }
    void connect(String token,Consumer<JsonNode> events,Consumer<String> status) {
        disconnect();int generation=epoch.get();attempt(token,events,status,generation,0);
    }
    private void attempt(String token,Consumer<JsonNode> events,Consumer<String> status,int generation,int retry) {
        if(epoch.get()!=generation) return;
        status.accept(retry==0?"Conectando…":"Reconectando…");
        URI uri=URI.create(base.toString().replaceFirst("^http","ws")+"/ws");
        http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(5)).header("Authorization","Bearer "+token)
            .buildAsync(uri,new WebSocket.Listener() {
                final StringBuilder buffer=new StringBuilder();
                @Override public void onOpen(WebSocket ws) {
                    if(epoch.get()!=generation){ws.abort();return;}
                    socket=ws;status.accept("Conectado");ws.request(1);
                }
                @Override public CompletionStage<?> onText(WebSocket ws,CharSequence text,boolean last) {
                    buffer.append(text);
                    if(buffer.length()>65536){ws.abort();return null;}
                    if(last) {
                        try { if(epoch.get()==generation) events.accept(ApiClient.JSON.readTree(buffer.toString())); }
                        catch(Exception ignored) {}
                        buffer.setLength(0);
                    }
                    ws.request(1);return null;
                }
                @Override public CompletionStage<?> onClose(WebSocket ws,int code,String reason) {
                    if(code==1008) { if(epoch.get()==generation) status.accept("Sessão expirada"); }
                    else reconnect();
                    return null;
                }
                @Override public void onError(WebSocket ws,Throwable error) { reconnect(); }
                private void reconnect() {
                    if(epoch.get()==generation) scheduler.schedule(()->attempt(token,events,status,generation,retry+1),Math.min(30,1L<<Math.min(retry,5)),TimeUnit.SECONDS);
                }
            }).exceptionally(error->{
                if(epoch.get()==generation) {
                    if(error.getCause() instanceof WebSocketHandshakeException handshake&&handshake.getResponse().statusCode()==401)
                        status.accept("Sessão expirada");
                    else scheduler.schedule(()->attempt(token,events,status,generation,retry+1),Math.min(30,1L<<Math.min(retry,5)),TimeUnit.SECONDS);
                }
                return null;
            });
    }
    void disconnect() { epoch.incrementAndGet();var old=socket;socket=null;if(old!=null) old.abort(); }
    @Override public void close() { disconnect();scheduler.shutdownNow(); }
}
