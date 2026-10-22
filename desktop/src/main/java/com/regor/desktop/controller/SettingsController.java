package com.regor.desktop.controller;
import com.regor.desktop.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.*;
public final class SettingsController extends BaseController {
    @FXML private TabPane settingsTabs;
    @FXML private Spinner<Integer> minAge,maxAge,maxDistance;
    @FXML private TextField interest,cityField,gender;
    @FXML private CheckBox matchesNotif,messagesNotif,showDistance,showOnline,discoverable;
    @FXML private ComboBox<String> theme;
    @FXML private PasswordField currentPassword,newPassword;
    @FXML private Label account;
    @FXML private Button save,password;
    public SettingsController(AppContext context){super(context);}
    @FXML private void initialize(){
        minAge.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(18,120,18));
        maxAge.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(18,120,60));
        maxDistance.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,20000,100));
        theme.getItems().setAll("escuro","claro");
    }
    @Override public void show(){
        task(()->context.api.get("/api/preferences"),p->{
            minAge.getValueFactory().setValue(p.path("minAge").asInt());maxAge.getValueFactory().setValue(p.path("maxAge").asInt());
            maxDistance.getValueFactory().setValue(p.path("maxDistanceKm").asInt());
            interest.setText(p.path("interest").asText());cityField.setText(p.path("location").asText());gender.setText(p.path("gender").asText());
            matchesNotif.setSelected(p.path("matchesNotif").asBoolean());messagesNotif.setSelected(p.path("messagesNotif").asBoolean());
            showDistance.setSelected(p.path("showDistance").asBoolean());showOnline.setSelected(p.path("showOnline").asBoolean());
            discoverable.setSelected(p.path("discoverable").asBoolean());theme.setValue(p.path("theme").asText());
        },save);
        context.worker.submit(()->{try{
            var u=context.api.get("/api/users/me");
            javafx.application.Platform.runLater(()->{if(active)account.setText(u.path("email").asText()+"  ·  "+(u.path("vip").asBoolean()?"VIP":"Conta gratuita")
                +"  ·  "+u.path("coins").asLong()+" moedas");});
        }catch(Exception e){javafx.application.Platform.runLater(()->{if(active)message(e.getMessage());});}});
    }
    @FXML private void save(){
        var data=new LinkedHashMap<String,Object>();
        data.put("minAge",minAge.getValue());data.put("maxAge",maxAge.getValue());data.put("maxDistanceKm",maxDistance.getValue());
        data.put("interest",interest.getText());data.put("gender",gender.getText());data.put("location",cityField.getText());
        data.put("matchesNotif",matchesNotif.isSelected());data.put("messagesNotif",messagesNotif.isSelected());
        data.put("showDistance",showDistance.isSelected());data.put("showOnline",showOnline.isSelected());
        data.put("discoverable",discoverable.isSelected());data.put("theme",theme.getValue());data.put("language","pt-BR");
        task(()->context.api.put("/api/preferences",data),p->{message("Preferências salvas.");context.navigator.theme();},save);
    }
    @FXML private void password(){
        var data=Map.of("currentPassword",currentPassword.getText(),"newPassword",newPassword.getText());
        task(()->context.api.put("/api/users/me/password",data),p->{currentPassword.clear();newPassword.clear();context.navigator.logout();},password);
    }
    @FXML private void section(javafx.event.ActionEvent event){settingsTabs.getSelectionModel().select(Integer.parseInt(((javafx.scene.Node)event.getSource()).getUserData().toString()));}
    @FXML private void editProfile(){context.navigator.go("profile");}
}
