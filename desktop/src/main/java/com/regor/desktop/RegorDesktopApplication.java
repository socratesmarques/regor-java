package com.regor.desktop;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.util.Objects;
public final class RegorDesktopApplication extends Application {
    private AppContext context;
    public RegorDesktopApplication() {}
    @Override public void start(Stage stage) {
        context=new AppContext();
        var scene=new Scene(new StackPane(),1280,840);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/regor/desktop/css/application.css")).toExternalForm());
        new Navigator(context,scene).go("landing");
        stage.setTitle("Regor");stage.setMinWidth(1024);stage.setMinHeight(700);stage.setScene(scene);stage.show();
    }
    @Override public void stop() { if(context!=null)context.close(); }
    public static void main(String[] args) { launch(args); }
}
