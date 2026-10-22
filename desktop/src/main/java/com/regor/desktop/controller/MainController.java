package com.regor.desktop.controller;
import com.regor.desktop.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
public final class MainController extends BaseController {
    @FXML private StackPane pageContent;
    @FXML private VBox navigation;
    @FXML private Label name,connection;
    public MainController(AppContext context) { super(context); }
    @Override public void show() { task(()->context.api.get("/api/profiles/me"),p->name.setText(p.path("displayName").asText())); }
    @FXML private void navigate(ActionEvent event) { context.navigator.go(((Node)event.getSource()).getUserData().toString()); }
    @FXML private void logout() { context.navigator.requestLogout(); }
    public void connection(String text) { connection.setText(text); }
    public void content(Parent root,String page) {
        pageContent.getChildren().setAll(root);
        for(Node node:navigation.getChildren()) {
            node.getStyleClass().remove("nav-active");
            if(page.equals(node.getUserData()))node.getStyleClass().add("nav-active");
        }
    }
}
