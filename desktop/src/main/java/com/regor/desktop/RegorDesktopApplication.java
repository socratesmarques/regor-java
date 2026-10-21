package com.regor.desktop;

import java.io.IOException;
import java.util.Objects;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class RegorDesktopApplication extends Application {
    public RegorDesktopApplication() {
    }

    @Override
    public void start(Stage stage) throws IOException {
        var view = Objects.requireNonNull(
                getClass().getResource("/com/regor/desktop/view/main-view.fxml"),
                "Recurso main-view.fxml não encontrado");
        var stylesheet = Objects.requireNonNull(
                getClass().getResource("/com/regor/desktop/css/application.css"),
                "Recurso application.css não encontrado");
        var scene = new Scene(new FXMLLoader(view).load(), 1200, 800);
        scene.getStylesheets().add(stylesheet.toExternalForm());
        stage.setTitle("Regor");
        stage.setMinWidth(800);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
