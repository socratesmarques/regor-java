package com.regor.desktop.controller;
import com.regor.desktop.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.time.LocalDate;
import java.util.*;

public final class ProfileController extends BaseController {
    @FXML private TextField displayName,username,cityField,interests,gender,latitude,longitude;
    @FXML private DatePicker birthDate;
    @FXML private TextArea bio;
    @FXML private VBox preview;
    @FXML private FlowPane photos;
    @FXML private Button save,upload;
    public ProfileController(AppContext context) { super(context); }
    @Override public void show() {
        task(()->context.api.get("/api/profiles/me"),p->{
            displayName.setText(p.path("displayName").asText());username.setText(p.path("username").asText());
            cityField.setText(p.path("location").asText());interests.setText(p.path("interests").asText());gender.setText(p.path("gender").asText());
            latitude.setText(p.path("latitude").isNull()?"":p.path("latitude").asText());
            longitude.setText(p.path("longitude").isNull()?"":p.path("longitude").asText());
            birthDate.setValue(LocalDate.parse(p.path("birthDate").asText()));bio.setText(p.path("bio").asText());
            preview.getChildren().setAll(Ui.person(context,p,440,260));
            photos.getChildren().clear();
            p.path("photos").forEach(id->{
                var data=com.regor.desktop.service.ApiClient.JSON.createObjectNode();data.putArray("photos").add(id.asText());
                var box=new VBox(8,Ui.picture(context,data,150,120));
                box.getChildren().add(Ui.button("Remover",()->task(()->context.api.delete("/api/photos/"+id.asText()),v->show(),box),"secondary-button"));
                photos.getChildren().add(box);
            });
        },save,upload);
    }
    @FXML private void save() {
        if(birthDate.getValue()==null){message("Informe sua data de nascimento");return;}
        var data=new LinkedHashMap<String,Object>();
        data.put("displayName",displayName.getText());data.put("username",username.getText());data.put("birthDate",birthDate.getValue().toString());
        data.put("location",cityField.getText());data.put("bio",bio.getText());data.put("interests",interests.getText());data.put("gender",gender.getText());
        try { data.put("latitude",latitude.getText().isBlank()?null:Double.valueOf(latitude.getText().replace(',','.')));
            data.put("longitude",longitude.getText().isBlank()?null:Double.valueOf(longitude.getText().replace(',','.'))); }
        catch(NumberFormatException e){message("Coordenadas inválidas");return;}
        task(()->context.api.put("/api/profiles/me",data),p->{preview.getChildren().setAll(Ui.person(context,p,440,260));message("Perfil salvo.");},save);
    }
    @FXML private void upload() {
        var chooser=new FileChooser();chooser.setTitle("Adicionar foto");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fotos JPEG e PNG","*.jpg","*.jpeg","*.png"));
        var selected=chooser.showOpenDialog(upload.getScene().getWindow());
        if(selected!=null)task(()->context.api.upload(selected.toPath()),p->show(),upload);
    }
}
