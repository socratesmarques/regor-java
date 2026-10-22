package com.regor.desktop.controller;
import com.regor.desktop.*;
import com.regor.desktop.service.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.Node;
import java.util.function.*;

public abstract class BaseController {
    protected final AppContext context;
    @FXML protected Label status;
    protected boolean active=true;
    protected BaseController(AppContext context) { this.context=context; }
    public void show() {}
    public void event(JsonNode event) {}
    public void dispose() { active=false; }
    protected void message(String text) { if(status!=null) status.setText(text); }
    protected <T> void task(Supplier<T> work,Consumer<T> success,Node... controls) {
        for(var node:controls) node.setDisable(true);
        message("Carregando…");
        context.worker.submit(()->{
            try {
                T result=work.get();
                Platform.runLater(()->{ if(!active)return;for(var node:controls)node.setDisable(false);message("");success.accept(result); });
            } catch(Exception e) {
                Platform.runLater(()->{
                    if(!active)return;for(var node:controls)node.setDisable(false);
                    message(e.getMessage()==null?"Não foi possível concluir a operação":e.getMessage());
                    if(e instanceof ApiClient.ApiException api&&api.status==401&&context.api.authenticated()) context.navigator.logout();
                });
            }
        });
    }
}
