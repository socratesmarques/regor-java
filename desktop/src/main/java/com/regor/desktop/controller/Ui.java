package com.regor.desktop.controller;
import com.regor.desktop.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayInputStream;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;

final class Ui {
    private Ui() {}
    static Label label(String text,String style) { var label=new Label(text);label.getStyleClass().add(style);label.setWrapText(true);return label; }
    static StackPane picture(AppContext context,JsonNode profile,double width,double height) {
        var pane=new StackPane();pane.setPrefSize(width,height);pane.setMinSize(width,height);pane.setMaxSize(width,height);
        pane.getStyleClass().add("photo");pane.getChildren().add(new com.regor.desktop.view.Icon("camera",42));
        var photos=profile.path("photos");
        if(photos.isArray()&&!photos.isEmpty()) {
            String id=photos.get(0).asText();
            context.worker.submit(()->{try {
                byte[] bytes=context.api.photo(id);
                var image=new Image(new ByteArrayInputStream(bytes),width,height,true,true);
                Platform.runLater(()->{var view=new ImageView(image);pane.getChildren().setAll(view);});
            }catch(Exception ignored) {}});
        }
        return pane;
    }
    static VBox person(AppContext context,JsonNode profile,double width,double height) {
        var card=new VBox(10);card.setMaxWidth(width);card.getStyleClass().add("person-card");
        card.getChildren().add(picture(context,profile,width,height));
        if(profile.path("photos").size()>1) {
            var gallery=new HBox(8);int index=1;
            for(var id:profile.path("photos")) {
                var photo=com.regor.desktop.service.ApiClient.JSON.createObjectNode();photo.putArray("photos").add(id.asText());
                gallery.getChildren().add(button(Integer.toString(index++),()->card.getChildren().set(0,picture(context,photo,width,height)),"secondary-button"));
            }
            card.getChildren().add(gallery);
        }
        String badges=(profile.path("verified").asBoolean()?" ✓":"")+(profile.path("vip").asBoolean()?" · VIP":"");
        card.getChildren().addAll(label(profile.path("displayName").asText()+", "+profile.path("age").asInt()+badges,"card-title"),
            label(profile.path("location").asText()+(profile.path("distanceKm").isNumber()?" · "+profile.path("distanceKm").asText()+" km":"")+(profile.path("online").asBoolean()?" · Online":""),"muted-text"),label(profile.path("bio").asText(),"body-text"),
            label(profile.path("interests").asText(),"accent-text"));
        return card;
    }
    static Button button(String text,Runnable action,String style) {
        var button=new Button(text);
        if(text.endsWith("  ›")){button.setText(text.substring(0,text.length()-3));button.setGraphic(new com.regor.desktop.view.Icon("chat",18));}
        button.getStyleClass().add(style);button.setOnAction(e->action.run());return button;
    }
}
