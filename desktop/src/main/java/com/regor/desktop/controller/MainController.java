package com.regor.desktop.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;

public final class MainController {
    @FXML
    private void closeApplication() {
        Platform.exit();
    }
}
