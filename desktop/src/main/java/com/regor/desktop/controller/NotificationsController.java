package com.regor.desktop.controller;
import com.regor.desktop.*;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
public final class NotificationsController extends BaseController {
    @FXML private VBox notifications;
    @FXML private Button next,previous;
    private int page;
    public NotificationsController(AppContext context) {super(context);}
    @Override public void show() {
        task(()->context.api.get("/api/notifications?page="+page),data->{
            notifications.getChildren().clear();next.setDisable(data.size()<50);previous.setDisable(page==0);
            if(data.isEmpty())notifications.getChildren().add(Ui.label("Nenhuma notificação recente","muted-text"));
            data.forEach(n->{
                String icon=switch(n.path("type").asText()){case "MATCH"->"heart";case "SUPERLIKE"->"star";case "MESSAGE"->"chat";default->"heart";};
                var row=new HBox(18);row.getStyleClass().add(n.path("readAt").isNull()?"notification-unread":"card");
                var text=new VBox(6,Ui.label(n.path("text").asText(),"card-title"),Ui.label(n.path("createdAt").asText(),"muted-text"));
                HBox.setHgrow(text,Priority.ALWAYS);
                row.getChildren().addAll(new com.regor.desktop.view.Icon(icon,24),text);
                if(n.path("readAt").isNull())row.getChildren().add(Ui.button("Marcar lida",()->task(
                    ()->context.api.put("/api/notifications/"+n.path("id").asText()+"/read",null),v->show(),row),"secondary-button"));
                if(!n.path("chatId").isNull())row.getChildren().add(Ui.button("Conversar",()->context.navigator.chat(n.path("chatId").asText()),"primary-button"));
                notifications.getChildren().add(row);
            });
        },next,previous);
    }
    @FXML private void next(){page++;show();}
    @FXML private void previous(){if(page>0)page--;show();}
    @Override public void event(JsonNode event){if(event.path("type").asText().equals("notification")||event.path("type").asText().equals("reconnected"))show();}
}
