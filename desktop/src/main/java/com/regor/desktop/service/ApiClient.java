package com.regor.desktop.service;
import com.fasterxml.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public final class ApiClient implements AutoCloseable {
    public static final ObjectMapper JSON=new ObjectMapper();
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final URI base;
    private volatile String token;
    private volatile String userId;
    private final RealtimeClient realtime;
    public ApiClient() { this(System.getenv().getOrDefault("REGOR_API_URL","http://localhost:8080")); }
    public ApiClient(String url) {
        base=URI.create(url.replaceAll("/+$",""));
        if(base.getHost()==null||base.getUserInfo()!=null||base.getQuery()!=null||base.getFragment()!=null
            ||!Set.of("https","http").contains(base.getScheme()))
            throw new IllegalArgumentException("REGOR_API_URL inválida");
        if(base.getScheme().equals("http")&&!Set.of("localhost","127.0.0.1","[::1]").contains(base.getHost()))
            throw new IllegalArgumentException("Use HTTPS para servidores externos");
        realtime=new RealtimeClient(http,base);
    }
    public void authenticate(JsonNode session) { token=session.path("token").asText();userId=session.path("user").path("id").asText(); }
    public boolean authenticated() { return token!=null; }
    public String userId() { return userId; }
    public void logout() { realtime.disconnect();token=null;userId=null; }
    public void realtime(java.util.function.Consumer<JsonNode> events,java.util.function.Consumer<String> status) {
        realtime.connect(token,events,status);
    }
    public JsonNode get(String path) { return request("GET",path,null); }
    public JsonNode post(String path,Object body) { return request("POST",path,body); }
    public JsonNode put(String path,Object body) { return request("PUT",path,body); }
    public JsonNode delete(String path) { return request("DELETE",path,null); }
    public JsonNode request(String method,String path,Object body) {
        try {
            var builder=builder(path);
            var payload=body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body));
            var response=http.send(builder.header("Content-Type","application/json").method(method,payload).build(),HttpResponse.BodyHandlers.ofByteArray());
            byte[] bytes=check(response);return bytes.length==0?JSON.nullNode():JSON.readTree(bytes);
        } catch(ApiException e) { throw e; }
        catch(Exception e) { throw new ApiException(0,"Não foi possível conectar ao servidor. Confira a conexão e tente novamente.",e); }
    }
    public byte[] photo(String id) {
        try { return check(http.send(builder("/api/photos/"+id).GET().build(),HttpResponse.BodyHandlers.ofByteArray())); }
        catch(ApiException e) { throw e; }
        catch(Exception e) { throw new ApiException(0,"Não foi possível carregar a foto",e); }
    }
    public JsonNode upload(Path path) {
        try {
            if(Files.size(path)>5*1024*1024) throw new ApiException(400,"Selecione uma foto de até 5 MB",null);
            String boundary="Regor"+UUID.randomUUID();
            byte[] start=("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"photo\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8);
            byte[] end=("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8);
            var payload=HttpRequest.BodyPublishers.concat(HttpRequest.BodyPublishers.ofByteArray(start),
                HttpRequest.BodyPublishers.ofFile(path),HttpRequest.BodyPublishers.ofByteArray(end));
            return JSON.readTree(check(http.send(builder("/api/photos").header("Content-Type","multipart/form-data; boundary="+boundary)
                .POST(payload).build(),HttpResponse.BodyHandlers.ofByteArray())));
        } catch(ApiException e) { throw e; }
        catch(Exception e) { throw new ApiException(0,"Não foi possível enviar a foto",e); }
    }
    private HttpRequest.Builder builder(String path) {
        var builder=HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(20));
        String auth=token;if(auth!=null) builder.header("Authorization","Bearer "+auth);
        return builder;
    }
    private byte[] check(HttpResponse<byte[]> response) {
        if(response.statusCode()/100==2) return response.body();
        String message=response.statusCode()==401?"Sessão expirada ou credenciais inválidas. Entre novamente.":"Não foi possível concluir a operação";
        try { message=JSON.readTree(response.body()).path("message").asText(message); } catch(Exception ignored) {}
        throw new ApiException(response.statusCode(),message,null);
    }
    public static String encode(String value) { return URLEncoder.encode(value,StandardCharsets.UTF_8); }
    @Override public void close() { logout();realtime.close();http.close(); }
    public static final class ApiException extends RuntimeException {
        private static final long serialVersionUID=1L;
        public final int status;
        public ApiException(int status,String message,Throwable cause) { super(message,cause);this.status=status; }
    }
}
