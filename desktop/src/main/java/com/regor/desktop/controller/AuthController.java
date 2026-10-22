package com.regor.desktop.controller;
import com.regor.desktop.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.*;

public final class AuthController extends BaseController {
    @FXML private TextField email,displayName,username;
    @FXML private PasswordField password,confirmation;
    @FXML private DatePicker birthDate;
    @FXML private Button submit;
    public AuthController(AppContext context) { super(context); }
    @FXML private void login() {
        var data=Map.of("email",email.getText().strip(),"password",password.getText());
        task(()->context.api.post("/api/auth/login",data),session->{
            context.api.authenticate(session);password.clear();context.navigator.authenticated();
        },submit);
    }
    @FXML private void register() {
        if(!password.getText().equals(confirmation.getText())) { message("As senhas não coincidem");return; }
        if(birthDate.getValue()==null) { message("Informe sua data de nascimento");return; }
        var data=Map.of("email",email.getText().strip(),"password",password.getText(),"displayName",displayName.getText(),
            "username",username.getText(),"birthDate",birthDate.getValue().toString());
        task(()->context.api.post("/api/auth/register",data),session->{
            context.api.authenticate(session);password.clear();confirmation.clear();context.navigator.authenticated();
        },submit);
    }
    @FXML private void openRegister() { context.navigator.go("register"); }
    @FXML private void openLogin() { context.navigator.go("login"); }
    @FXML private void landing() { context.navigator.go("landing"); }
}
