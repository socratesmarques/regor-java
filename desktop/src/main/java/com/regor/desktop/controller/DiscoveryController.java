package com.regor.desktop.controller;
import com.regor.desktop.*;
import com.regor.desktop.service.ApiClient;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;

public final class DiscoveryController extends BaseController {
    @FXML private FlowPane cards;
    @FXML private TextField query,cityField,interest,gender,distance;
    @FXML private Spinner<Integer> minAge,maxAge;
    @FXML private Button search,next,previous;
    @FXML private Label count;
    private int page;
    public DiscoveryController(AppContext context) { super(context); }
    @FXML private void initialize() {
        minAge.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(18,120,18));
        maxAge.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(18,120,60));
    }
    @Override public void show() {
        task(()->context.api.get("/api/preferences"),p->{
            minAge.getValueFactory().setValue(p.path("minAge").asInt());maxAge.getValueFactory().setValue(p.path("maxAge").asInt());
            cityField.setText(p.path("location").asText());interest.setText(p.path("interest").asText());gender.setText(p.path("gender").asText());load();
        });
    }
    @FXML private void search() { page=0;load(); }
    @FXML private void next() { page++;load(); }
    @FXML private void previous() { if(page>0)page--;load(); }
    private void load() {
        if(!distance.getText().isBlank()&&!distance.getText().matches("[0-9]{1,5}")) {message("Distância deve ser um número em km");return;}
        String path="/api/discovery?page="+page+"&q="+ApiClient.encode(query.getText())+"&minAge="+minAge.getValue()+"&maxAge="+maxAge.getValue()
            +"&location="+ApiClient.encode(cityField.getText())+"&interest="+ApiClient.encode(interest.getText())+"&gender="+ApiClient.encode(gender.getText())
            +(distance.getText().isBlank()?"":"&distance="+distance.getText());
        task(()->context.api.get(path),data->{
            cards.getChildren().clear();count.setText(data.size()+" perfis · página "+(page+1));
            previous.setDisable(page==0);next.setDisable(data.size()<30);
            if(data.isEmpty())cards.getChildren().add(Ui.label("Nenhum perfil encontrado. Experimente outros filtros.","muted-text"));
            data.forEach(p->{
                var card=Ui.person(context,p,260,225);
                var actions=new HBox(8);
                for(String kind:List.of("DISLIKE","SUPERLIKE","LIKE")) {
                    String text=switch(kind){case "LIKE"->"♥";case "SUPERLIKE"->"★";default->"✕";};
                    var button=Ui.button(text,()->task(()->context.api.post("/api/swipes",Map.of("targetId",p.path("id").asText(),"kind",kind)),
                        result->{cards.getChildren().remove(card);message(result.path("matchId").isNull()?"Salvo":"É um match! Abra Chats para conversar.");},card),
                        kind.equals("LIKE")?"primary-button":"secondary-button");
                    button.setTooltip(new Tooltip(kind.equals("LIKE")?"Curtir":kind.equals("SUPERLIKE")?"Super Like":"Não curtir"));
                    actions.getChildren().add(button);
                }
                card.getChildren().add(actions);cards.getChildren().add(card);
            });
        },search,next,previous);
    }
}
