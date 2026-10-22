package com.regor.desktop.controller;
import com.regor.desktop.*;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;

public final class HomeController extends BaseController {
    @FXML private VBox profile,matches;
    @FXML private Button like,dislike,superlike;
    private final Deque<JsonNode> queue=new ArrayDeque<>();
    public HomeController(AppContext context) { super(context); }
    @Override public void show() {
        task(()->context.api.get("/api/discovery"),data->{queue.clear();data.forEach(queue::add);render();});
        loadMatches();
    }
    private void render() {
        profile.getChildren().clear();
        boolean empty=queue.isEmpty();
        like.setDisable(empty);dislike.setDisable(empty);superlike.setDisable(empty);
        if(empty) profile.getChildren().addAll(Ui.label("Você viu todos os perfis disponíveis","card-title"),
            Ui.label("Ajuste suas preferências ou volte mais tarde para conhecer novas pessoas.","muted-text"));
        else profile.getChildren().add(Ui.person(context,queue.peek(),420,360));
    }
    private void loadMatches() {
        context.worker.submit(()->{try {
            var data=context.api.get("/api/matches");
            javafx.application.Platform.runLater(()->{
                if(!active)return;matches.getChildren().clear();
                if(data.isEmpty()) matches.getChildren().add(Ui.label("Seus matches aparecerão aqui quando a curtida for recíproca.","muted-text"));
                data.forEach(m->matches.getChildren().add(Ui.button(m.path("profile").path("displayName").asText()+"  ›",
                    ()->context.navigator.chat(m.path("chatId").asText()),"match-button")));
            });
        }catch(Exception e){javafx.application.Platform.runLater(()->{if(active)message(e.getMessage());});}});
    }
    private void swipe(String kind) {
        if(queue.isEmpty())return;
        var data=Map.of("targetId",queue.peek().path("id").asText(),"kind",kind);
        task(()->context.api.post("/api/swipes",data),result->{
            queue.remove();render();
            if(!result.path("matchId").isNull()) { message("É um match! Vocês já podem conversar.");loadMatches(); }
        },like,dislike,superlike);
    }
    @FXML private void like() { swipe("LIKE"); }
    @FXML private void dislike() { swipe("DISLIKE"); }
    @FXML private void superlike() { swipe("SUPERLIKE"); }
    @FXML private void refresh() { show(); }
    @Override public void event(JsonNode event) { if(event.path("type").asText().equals("notification")||event.path("type").asText().equals("reconnected")) loadMatches(); }
}
