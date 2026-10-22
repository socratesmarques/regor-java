package com.regor.desktop.controller;

import com.regor.desktop.*;
import com.regor.desktop.view.Icon;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.time.*;
import java.util.*;
import java.util.function.*;

public final class ProfileController extends BaseController {
    @FXML private TextField displayName,username,cityField,interests,gender,latitude,longitude;
    @FXML private DatePicker birthDate;
    @FXML private TextArea bio;
    @FXML private VBox editor;
    @FXML private StackPane previewPhoto;
    @FXML private FlowPane photos,interestSuggestions;
    @FXML private Button save,cancel,upload;
    @FXML private Label previewName,previewLocation,previewBio,previewInterests,bioCount,photoCount,saveHint;
    private final BooleanProperty dirty=new SimpleBooleanProperty(false),busy=new SimpleBooleanProperty(false),loaded=new SimpleBooleanProperty(false);
    private final Map<String,ToggleButton> chips=new LinkedHashMap<>();
    private JsonNode saved;
    private boolean filling;
    private int photoTotal;

    public ProfileController(AppContext context) { super(context); }
    @FXML private void initialize() {
        save.disableProperty().bind(dirty.not().or(busy));
        cancel.disableProperty().bind(save.disableProperty());
        editor.disableProperty().bind(loaded.not().or(busy));
        for(var field:List.of(displayName,username,cityField,interests,gender,latitude,longitude,bio))
            field.textProperty().addListener((o,a,b)->changed());
        var dateFormat=java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT);
        birthDate.setConverter(new javafx.util.StringConverter<LocalDate>() {
            @Override public String toString(LocalDate date){return date==null?"":dateFormat.format(date);}
            @Override public LocalDate fromString(String text){return text==null||text.isBlank()?null:LocalDate.parse(text.strip(),dateFormat);}
        });
        birthDate.getEditor().textProperty().addListener((o,a,b)->changed());
        displayName.setAccessibleText("Nome de exibição");username.setAccessibleText("Nome de usuário");
        cityField.setAccessibleText("Cidade");gender.setAccessibleText("Gênero opcional");
        bio.setAccessibleText("Sua história");interests.setAccessibleText("Interesses separados por vírgula");
        latitude.setAccessibleText("Latitude privada");longitude.setAccessibleText("Longitude privada");
        birthDate.setAccessibleText("Data de nascimento, dia mês e ano");
        birthDate.valueProperty().addListener((o,a,b)->changed());
        limit(displayName,80);limit(username,40);limit(cityField,120);limit(gender,30);limit(bio,2000);limit(interests,500);
        for(String interest:List.of("Música","Viagens","Café","Cinema","Natureza","Jogos","Arte","Gastronomia")) {
            var chip=new ToggleButton(interest);chip.getStyleClass().add("interest-chip");
            chip.setOnAction(e->{
                var values=new ArrayList<>(interestValues());
                values.removeIf(v->v.equalsIgnoreCase(interest));
                if(chip.isSelected())values.add(interest);
                interests.setText(String.join(", ",values));changed();
            });
            chips.put(interest,chip);interestSuggestions.getChildren().add(chip);
        }
    }
    private static void limit(TextInputControl field,int max) {
        field.setTextFormatter(new TextFormatter<String>(change->change.getControlNewText().length()<=max?change:null));
    }
    @Override public void show() {
        run(()->context.api.get("/api/profiles/me"),p->{saved=p;fill();renderPhotos(p);loaded.set(true);});
    }
    private <T> void run(Supplier<T> work,Consumer<T> success) {
        busy.set(true);
        task(()->{try{return work.get();}finally{Platform.runLater(()->busy.set(false));}},success);
    }
    private void fill() {
        filling=true;
        displayName.setText(saved.path("displayName").asText());username.setText(saved.path("username").asText());
        cityField.setText(saved.path("location").asText());interests.setText(saved.path("interests").asText());gender.setText(saved.path("gender").asText());
        latitude.setText(saved.path("latitude").isNull()?"":saved.path("latitude").asText());
        longitude.setText(saved.path("longitude").isNull()?"":saved.path("longitude").asText());
        birthDate.setValue(LocalDate.parse(saved.path("birthDate").asText()));bio.setText(saved.path("bio").asText());
        filling=false;changed();
    }
    private List<String> interestValues() {
        return Arrays.stream(interests.getText().split(",")).map(String::strip).filter(s->!s.isEmpty()).distinct().toList();
    }
    private void changed() {
        if(filling)return;
        previewName.setText(displayName.getText().isBlank()?"Seu nome":displayName.getText());
        if(birthDate.getValue()!=null)previewName.setText(previewName.getText()+", "+Period.between(birthDate.getValue(),LocalDate.now()).getYears());
        previewLocation.setText(cityField.getText().isBlank()?"Sua cidade":cityField.getText());
        previewBio.setText(bio.getText().isBlank()?"Sua história aparece aqui.":bio.getText());
        previewInterests.setText(interests.getText().isBlank()?"Seus interesses":interests.getText());
        bioCount.setText(bio.getText().length()+" / 2000");
        chips.forEach((text,chip)->chip.setSelected(interestValues().stream().anyMatch(text::equalsIgnoreCase)));
        if(saved!=null)dirty.set(!same("displayName",displayName)||!same("username",username)||!same("location",cityField)
            ||!same("interests",interests)||!same("gender",gender)||!same("bio",bio)
            ||!Objects.equals(birthDate.getValue(),LocalDate.parse(saved.path("birthDate").asText()))
            ||!birthDate.getEditor().getText().equals(birthDate.getConverter().toString(LocalDate.parse(saved.path("birthDate").asText())))
            ||!sameCoordinate("latitude",latitude)||!sameCoordinate("longitude",longitude));
        saveHint.setText(dirty.get()?"Você tem alterações não salvas":"Seu perfil está atualizado");
    }
    private boolean same(String key,TextInputControl field) { return field.getText().equals(saved.path(key).asText()); }
    private boolean sameCoordinate(String key,TextField field) {
        try {return Objects.equals(coordinate(field),saved.path(key).isNull()?null:saved.path(key).doubleValue());}
        catch(NumberFormatException e){return false;}
    }
    private static Double coordinate(TextField field) { return field.getText().isBlank()?null:Double.valueOf(field.getText().strip().replace(',','.')); }
    @FXML private void cancel() {fill();success("Alterações descartadas. Suas fotos foram mantidas.");}
    @FXML private void save() {
        try {birthDate.setValue(birthDate.getConverter().fromString(birthDate.getEditor().getText()));}
        catch(RuntimeException e){invalid(birthDate,"Use uma data válida no formato DD/MM/AAAA.");return;}
        if(displayName.getText().isBlank()){invalid(displayName,"Informe seu nome de exibição.");return;}
        if(!username.getText().matches("[a-zA-Z0-9_]{3,40}")){invalid(username,"Use de 3 a 40 letras, números ou _ no nome de usuário.");return;}
        if(birthDate.getValue()==null||Period.between(birthDate.getValue(),LocalDate.now()).getYears()<18||Period.between(birthDate.getValue(),LocalDate.now()).getYears()>120){invalid(birthDate,"Informe uma data de nascimento válida. É preciso ter 18 anos.");return;}
        Double lat,lon;
        try {lat=coordinate(latitude);lon=coordinate(longitude);
            if((lat==null)!=(lon==null)||(lat!=null&&(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)))throw new NumberFormatException();
        }catch(NumberFormatException e){message("Informe latitude (-90 a 90) e longitude (-180 a 180) juntas, ou deixe ambas vazias.");return;}
        var data=new LinkedHashMap<String,Object>();
        data.put("displayName",displayName.getText());data.put("username",username.getText());data.put("birthDate",birthDate.getValue().toString());
        data.put("location",cityField.getText());data.put("bio",bio.getText());data.put("interests",interests.getText());data.put("gender",gender.getText());
        data.put("latitude",lat);data.put("longitude",lon);
        run(()->context.api.put("/api/profiles/me",data),p->{saved=p;fill();context.navigator.refreshProfile();success("Pronto! Seu perfil foi atualizado.");});
    }
    @Override public boolean canLeave() {
        if(busy.get())return false;
        if(!dirty.get())return true;
        var stay=new ButtonType("Continuar editando",ButtonBar.ButtonData.CANCEL_CLOSE);
        var leave=new ButtonType("Descartar e sair",ButtonBar.ButtonData.OK_DONE);
        var dialog=new Alert(Alert.AlertType.CONFIRMATION,"Salve as alterações para publicar seu perfil.",stay,leave);
        dialog.setTitle("Alterações não salvas");dialog.setHeaderText("Você ainda está editando seu perfil");
        dialog.initOwner(save.getScene().getWindow());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/regor/desktop/css/application.css").toExternalForm());
        return dialog.showAndWait().orElse(stay)==leave;
    }
    @Override protected void message(String text) {
        status.setGraphic(null);status.getStyleClass().remove("status-success");super.message(text);
    }
    private void success(String text) {
        message(text);status.getStyleClass().add("status-success");status.setGraphic(new Icon("check",14));
    }
    private void invalid(Control field,String text) {message(text);field.requestFocus();}
    private void renderPhotos(JsonNode p) {
        photoTotal=p.path("photos").size();photoCount.setText(photoTotal+" de 6");upload.setDisable(photoTotal>=6);
        previewPhoto.getChildren().setAll(Ui.picture(context,p,226,190));photos.getChildren().clear();
        if(photoTotal==0)photos.getChildren().add(Ui.label("Sua próxima conexão quer conhecer você. Adicione sua primeira foto.","section-caption"));
        int index=0;
        for(var id:p.path("photos")) {
            var data=com.regor.desktop.service.ApiClient.JSON.createObjectNode();data.putArray("photos").add(id.asText());
            var box=new VBox(4,Ui.picture(context,data,108,94));box.getStyleClass().add("photo-tile");
            var remove=Ui.button("Remover",()->removePhoto(id.asText()),"photo-remove");remove.setGraphic(new Icon("trash",14));
            remove.setAccessibleText("Remover foto "+(++index));
            box.getChildren().addAll(Ui.label(index==1?"Capa":"Foto "+index,"field-hint"),remove);photos.getChildren().add(box);
        }
    }
    private void removePhoto(String id) {
        var dialog=new Alert(Alert.AlertType.CONFIRMATION,"Esta foto será removida do seu perfil.",ButtonType.CANCEL,ButtonType.OK);
        dialog.setTitle("Remover foto");dialog.setHeaderText("Remover esta foto?");dialog.initOwner(upload.getScene().getWindow());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/regor/desktop/css/application.css").toExternalForm());
        if(dialog.showAndWait().orElse(ButtonType.CANCEL)!=ButtonType.OK)return;
        run(()->{context.api.delete("/api/photos/"+id);return context.api.get("/api/profiles/me");},p->{renderPhotos(p);success("Foto removida. Sua edição foi preservada.");});
    }
    @FXML private void upload() {
        if(photoTotal>=6)return;
        var chooser=new FileChooser();chooser.setTitle("Adicionar foto");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fotos JPEG e PNG","*.jpg","*.jpeg","*.png"));
        var selected=chooser.showOpenDialog(upload.getScene().getWindow());
        if(selected!=null)run(()->{context.api.upload(selected.toPath());return context.api.get("/api/profiles/me");},p->{renderPhotos(p);success("Foto adicionada. Sua edição foi preservada.");});
    }
}
