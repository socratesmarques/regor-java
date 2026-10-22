package com.regor.desktop;
import com.regor.desktop.controller.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.layout.*;
import java.io.IOException;
import java.util.*;

public final class Navigator {
    private final AppContext context;private final Scene scene;
    private BaseController current;private MainController shell;private Parent shellRoot;
    public Navigator(AppContext context,Scene scene) { this.context=context;this.scene=scene;context.navigator=this; }
    public void go(String page) {
        if(!Set.of("landing","login","register").contains(page)&&!context.api.authenticated()) page="login";
        if(current!=null) current.dispose();
        try {
            var loader=loader(page);Parent root=loader.load();current=loader.getController();
            if(Set.of("landing","login","register").contains(page)) {
                shell=null;shellRoot=null;scene.setRoot(root);
            } else {
                if(shell==null) { var layout=loader("main-view");shellRoot=layout.load();shell=layout.getController();shell.show(); }
                shell.content(root,page);scene.setRoot(shellRoot);
            }
            if(current!=null) current.show();
        } catch(IOException e) { throw new IllegalStateException("Não foi possível abrir a tela "+page,e); }
    }
    private FXMLLoader loader(String page) {
        var loader=new FXMLLoader(Objects.requireNonNull(getClass().getResource("/com/regor/desktop/view/"+page+".fxml")));
        loader.setControllerFactory(type->{try { return type.getConstructor(AppContext.class).newInstance(context); }
            catch(Exception e) { throw new IllegalStateException(e); }});
        return loader;
    }
    public void authenticated() {
        go("home");
        context.api.realtime(event->Platform.runLater(()->{ if(current!=null) current.event(event); }),
            status->Platform.runLater(()->{
                if(shell!=null) shell.connection(status);
                if(status.equals("Conectado") && current!=null) current.event(com.regor.desktop.service.ApiClient.JSON.createObjectNode().put("type","reconnected"));
                if(status.equals("Sessão expirada")) logout();
            }));
        theme();
    }
    public void theme() {
        context.worker.submit(()->{try {
            String mode=context.api.get("/api/preferences").path("theme").asText();
            Platform.runLater(()->{scene.getRoot().pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("light"),mode.equals("claro"));});
        } catch(Exception ignored) {}});
    }
    public void chat(String id) { go("chats");((ChatsController)current).openChat(id); }
    public void logout() { context.api.logout();if(shell!=null)shell.dispose();go("landing"); }
}
