package com.regor.desktop.controller;
import com.regor.desktop.*;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;

public final class ChatsController extends BaseController {
    @FXML private VBox conversations,messages;
    @FXML private ScrollPane messageScroll;
    @FXML private TextField content,search;
    @FXML private Label title;
    @FXML private Button send,older,moreMatches;
    private String chatId;
    private String pendingChat,pendingText,pendingId;
    private int page,matchPage;
    private final List<JsonNode> matches=new ArrayList<>();
    public ChatsController(AppContext context) { super(context); }
    @Override public void show() {
        loadMatches(0);
    }
    @FXML private void moreMatches() { loadMatches(matchPage+1); }
    private void loadMatches(int requestedPage) {
        task(()->context.api.get("/api/chats?page="+requestedPage),data->{
            if(requestedPage==0)matches.clear();
            data.forEach(m->{if(matches.stream().noneMatch(old->old.path("id").equals(m.path("id"))))matches.add(m);});
            matchPage=requestedPage;renderMatches();moreMatches.setDisable(data.size()<30);
            updateTitle();
            if(chatId!=null&&requestedPage==0)load(false);
        },moreMatches);
    }
    private void updateTitle() {
        if(chatId!=null)title.setText(matches.stream().filter(m->m.path("chatId").asText().equals(chatId))
            .map(m->m.path("profile").path("displayName").asText()).findFirst().orElse("Conversa"));
    }
    @FXML private void filter() { renderMatches(); }
    private void renderMatches() {
        conversations.getChildren().clear();
        if(matches.isEmpty())conversations.getChildren().add(Ui.label("Dê match para iniciar uma conversa.","muted-text"));
        matches.stream().filter(m->m.path("profile").path("displayName").asText().toLowerCase(Locale.ROOT).contains(search.getText().toLowerCase(Locale.ROOT)))
            .forEach(m->{
                var button=Ui.button(m.path("profile").path("displayName").asText()+"  ›",()->openChat(m.path("chatId").asText()),"match-button");
                button.setMaxWidth(Double.MAX_VALUE);conversations.getChildren().add(button);
            });
    }
    public void openChat(String id) {
        chatId=id;page=0;content.setDisable(false);send.setDisable(false);
        updateTitle();
        load(false);
    }
    private void load(boolean prepend) {
        if(chatId==null)return;
        String requested=chatId;int requestedPage=prepend?page+1:0;
        task(()->context.api.get("/api/chats/"+requested+"/messages?page="+requestedPage),data->{
            if(!requested.equals(chatId))return;
            page=requestedPage;
            if(!prepend)messages.getChildren().clear();
            var nodes=new ArrayList<javafx.scene.Node>();
            if(data.isEmpty()&&!prepend) nodes.add(Ui.label("Vocês deram match. Envie a primeira mensagem!","muted-text"));
            data.forEach(m->{
                boolean own=m.path("senderId").asText().equals(context.api.userId());
                var label=Ui.label(m.path("content").asText(),"message-text");label.setMaxWidth(400);
                var bubble=new VBox(6,label,Ui.label(m.path("createdAt").asText().replace('T',' ').substring(0,16)+" UTC","message-time"));
                bubble.getStyleClass().add(own?"message-own":"message-other");bubble.setMaxWidth(440);
                var row=new HBox(bubble);row.setAlignment(own?Pos.CENTER_RIGHT:Pos.CENTER_LEFT);nodes.add(row);
            });
            if(prepend)messages.getChildren().addAll(0,nodes);else messages.getChildren().addAll(nodes);
            older.setDisable(data.size()<50);
            if(!prepend)javafx.application.Platform.runLater(()->messageScroll.setVvalue(1));
        },older);
    }
    @FXML private void older() {load(true);}
    @FXML private void send() {
        if(chatId==null||content.getText().isBlank())return;
        if(content.getText().length()>4000){message("Use até 4000 caracteres");return;}
        String requested=chatId,text=content.getText();
        if(!requested.equals(pendingChat)||!text.equals(pendingText)) {
            pendingChat=requested;pendingText=text;pendingId=UUID.randomUUID().toString();
        }
        var body=Map.of("clientId",pendingId,"content",text);
        task(()->context.api.post("/api/chats/"+requested+"/messages",body),m->{
            pendingChat=null;pendingText=null;pendingId=null;
            if(requested.equals(chatId)){if(content.getText().equals(text))content.clear();page=0;load(false);}
        },send);
    }
    @Override public void event(JsonNode event) {
        if(event.path("type").asText().equals("reconnected")){page=0;show();}
        if(event.path("type").asText().equals("message")&&event.path("data").path("chatId").asText().equals(chatId)) {page=0;load(false);}
        if(event.path("type").asText().equals("notification")&&event.path("data").path("type").asText().equals("MATCH"))show();
    }
}
