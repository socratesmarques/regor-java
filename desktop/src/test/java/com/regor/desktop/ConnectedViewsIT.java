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
                if(page.equals("profile")) {
                    fx(()->{
                        ((TextField)scene.lookup("#displayName")).setText("Nome em edição");
                        assertTrue(((Label)scene.lookup("#previewName")).getText().startsWith("Nome em edição"));
                        assertFalse(((Button)scene.lookup("#save")).isDisabled());
                        Platform.runLater(()->{
                            for(var window:List.copyOf(javafx.stage.Window.getWindows())) {
                                var pane=window.getScene().lookup(".dialog-pane");
                                if(pane instanceof DialogPane dialog) {
                                    var cancel=dialog.getButtonTypes().stream().filter(b->b.getButtonData()==ButtonBar.ButtonData.CANCEL_CLOSE).findFirst().orElseThrow();
                                    ((Button)dialog.lookupButton(cancel)).fire();
                                }
                            }
                        });
                        context.navigator.go("home");
                        assertNotNull(scene.lookup("#editor"),"Sair foi cancelado; a edição deve continuar aberta");
                        assertEquals("Nome em edição",((TextField)scene.lookup("#displayName")).getText());
                        ((Button)scene.lookup("#cancel")).fire();
                        assertEquals("Alice",((TextField)scene.lookup("#displayName")).getText());
                        assertTrue(((Button)scene.lookup("#save")).isDisabled());
                        ((TextArea)scene.lookup("#bio")).setText("Café, trilhas e boas conversas. Sempre pronta para conhecer um lugar novo.");
                        ((Button)scene.lookup("#save")).fire();return null;
                    });
                    await(()->((Label)scene.lookup("#status")).getText().equals("Pronto! Seu perfil foi atualizado."));
                    assertEquals("Café, trilhas e boas conversas. Sempre pronta para conhecer um lugar novo.",context.api.get("/api/profiles/me").path("bio").asText());
                    fx(()->{
                        snapshot(scene,"profile-compact",1024,700);
                        var editor=scene.lookup("#editor");
                        assertTrue(editor.localToScene(editor.getBoundsInLocal()).getMinX()>=238);
                        assertTrue(scene.lookup("#previewPhoto").localToScene(scene.lookup("#previewPhoto").getBoundsInLocal()).getMaxX()<=1024);
                        return null;
                    });
                }
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
        snapshot(scene,name,1280,840);
    }
    private static void snapshot(Scene scene,String name,int width,int height) throws Exception {
        Parent root=scene.getRoot();((Region)root).resize(width,height);root.applyCss();root.layout();
        WritableImage image=root.snapshot(null,new WritableImage(width,height));
        Path path=Path.of("target","screenshots","connected-"+name+".ppm");Files.createDirectories(path.getParent());
        try(var output=new BufferedOutputStream(Files.newOutputStream(path))) {
            output.write(("P6\n"+width+" "+height+"\n255\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            var pixels=image.getPixelReader();
            for(int y=0;y<height;y++)for(int x=0;x<width;x++){int a=pixels.getArgb(x,y);output.write((a>>16)&255);output.write((a>>8)&255);output.write(a&255);}
        }
    }
}
