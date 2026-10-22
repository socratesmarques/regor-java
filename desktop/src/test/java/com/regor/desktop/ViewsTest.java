package com.regor.desktop;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import com.regor.desktop.controller.BaseController;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class ViewsTest {
    @BeforeAll static void toolkit() throws Exception {
        var ready=new CountDownLatch(1);Platform.startup(()->{Platform.setImplicitExit(false);ready.countDown();});
        assertTrue(ready.await(10,TimeUnit.SECONDS));
    }
    @AfterAll static void stop(){Platform.exit();}
    @Test void allScreensLoadAndRenderWithStylesAndControllers() throws Exception {
        var done=new CompletableFuture<Void>();
        Platform.runLater(()->{
            try(var context=new AppContext()) {
                for(String page:List.of("landing","login","register","main-view","home","discovery","profile","chats","notifications","settings")) {
                    var resource=getClass().getResource("/com/regor/desktop/view/"+page+".fxml");
                    assertNotNull(resource,page);
                    var loader=new FXMLLoader(resource);
                    loader.setControllerFactory(type->{try{return type.getConstructor(AppContext.class).newInstance(context);}catch(Exception e){throw new RuntimeException(e);}});
                    Parent root=loader.load();
                    var scene=new Scene(root,1280,840);
                    scene.getStylesheets().add(getClass().getResource("/com/regor/desktop/css/application.css").toExternalForm());
                    if(root instanceof Region r)r.resize(1280,840);
                    root.applyCss();root.layout();
                    WritableImage image=root.snapshot(null,new WritableImage(1280,840));
                    assertNotNull(image.getPixelReader());
                    Path output=Path.of("target","screenshots",page+".ppm");Files.createDirectories(output.getParent());
                    try(var stream=new BufferedOutputStream(Files.newOutputStream(output))) {
                        stream.write("P6\n1280 840\n255\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                        var reader=image.getPixelReader();
                        for(int y=0;y<840;y++)for(int x=0;x<1280;x++) {
                            int argb=reader.getArgb(x,y);stream.write((argb>>16)&255);stream.write((argb>>8)&255);stream.write(argb&255);
                        }
                    }
                    ((BaseController)loader.getController()).dispose();
                }
                done.complete(null);
            }catch(Throwable error){done.completeExceptionally(error);}
        });
        done.get(30,TimeUnit.SECONDS);
    }
}
