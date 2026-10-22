package com.regor.desktop.controller;
import com.regor.desktop.*;
import javafx.fxml.FXML;
public final class LandingController extends BaseController {
    @FXML private AuthController loginController;
    public LandingController(AppContext context) { super(context); }
    @FXML private void register() { context.navigator.go("register"); }
    @Override public void dispose() { super.dispose();if(loginController!=null)loginController.dispose(); }
}
