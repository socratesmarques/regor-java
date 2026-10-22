package com.regor.desktop;
import javafx.application.Platform;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.WritableImage;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Executado explicitamente contra UiFixtureServer, nunca contra dados reais. */
class ConnectedViewsIT {
    static <T> T fx(Callable<T> action) throws Exception {
        var result=new CompletableFuture<T>();
        Platform.runLater(()->{try{result.complete(action.call());}catch(Throwable e){result.completeExceptionally(e);}});
        return result.get(15,TimeUnit.SECONDS);
    }
    static void await(Callable<Boolean> ready) throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
        while(System.nanoTime()<deadline){if(fx(ready))return;Thread.sleep(100);}
        fail("A tela não terminou de carregar");
    }
    @Test void realLoginAllAuthenticatedScreensAndRegistration() throws Exception {
        assertNotNull(System.getenv("REGOR_API_URL"),"Execute contra UiFixtureServer");
        Platform.startup(()->Platform.setImplicitExit(false));
        try(var context=new AppContext()) {
            Scene scene=fx(()->{
                var s=new Scene(new StackPane(),1280,840);
                s.getStylesheets().add(getClass().getResource("/com/regor/desktop/css/application.css").toExternalForm());
                new Navigator(context,s).go("login");
                ((TextField)s.lookup("#email")).setText("alice@regor.test");
                ((PasswordField)s.lookup("#password")).setText("fixture-password");
                ((Button)s.lookup("#submit")).fire();return s;
            });
            await(()->{
                ((Region)scene.getRoot()).resize(1280,840);scene.getRoot().applyCss();scene.getRoot().layout();
                return context.api.authenticated()&&scene.lookup(".person-card")!=null;
            });
            String chat=context.api.get("/api/chats").get(0).path("chatId").asText();
            for(String page:List.of("home","discovery","profile","chats","notifications","settings")) {
                System.out.println("Validando tela conectada: "+page);
                fx(()->{if(page.equals("chats"))context.navigator.chat(chat);else context.navigator.go(page);return null;});
                await(()->{
                    ((Region)scene.getRoot()).resize(1280,840);scene.getRoot().applyCss();scene.getRoot().layout();
                    var label=(Label)scene.lookup("#status");
                    return label!=null&&!label.getText().equals("Carregando…");
                });
                assertEquals("",fx(()->((Label)scene.lookup("#status")).getText()),page);
                // Wait for asynchronous image decoding, independently of the API response.
                Thread.sleep(350);
                fx(()->{snapshot(scene,page);return null;});
            }
            fx(()->{
                context.navigator.logout();context.navigator.go("register");
                ((Region)scene.getRoot()).resize(1280,840);scene.getRoot().applyCss();scene.getRoot().layout();
                ((TextField)scene.lookup("#displayName")).setText("Nova Conta");
                ((TextField)scene.lookup("#username")).setText("nova_conta");
                ((TextField)scene.lookup("#email")).setText("nova@regor.test");
                ((DatePicker)scene.lookup("#birthDate")).setValue(java.time.LocalDate.of(1998,1,1));
                ((PasswordField)scene.lookup("#password")).setText("another-password");
                ((PasswordField)scene.lookup("#confirmation")).setText("another-password");
                ((Button)scene.lookup("#submit")).fire();return null;
            });
            await(()->context.api.authenticated());
            assertEquals("nova@regor.test",context.api.get("/api/users/me").path("email").asText());
        }finally{Platform.exit();}
    }
    private static void snapshot(Scene scene,String name) throws Exception {
        Parent root=scene.getRoot();((Region)root).resize(1280,840);root.applyCss();root.layout();
        WritableImage image=root.snapshot(null,new WritableImage(1280,840));
        Path path=Path.of("target","screenshots","connected-"+name+".ppm");Files.createDirectories(path.getParent());
        try(var output=new BufferedOutputStream(Files.newOutputStream(path))) {
            output.write("P6\n1280 840\n255\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            var pixels=image.getPixelReader();
            for(int y=0;y<840;y++)for(int x=0;x<1280;x++){int a=pixels.getArgb(x,y);output.write((a>>16)&255);output.write((a>>8)&255);output.write(a&255);}
        }
    }
}
