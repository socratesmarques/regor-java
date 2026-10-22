module com.regor.desktop {
    requires javafx.controls;
    requires transitive javafx.graphics;
    requires javafx.fxml;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;

    exports com.regor.desktop to javafx.graphics;
    opens com.regor.desktop.controller to javafx.fxml;
}
