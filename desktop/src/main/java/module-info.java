module com.regor.desktop {
    requires javafx.controls;
    requires transitive javafx.graphics;
    requires javafx.fxml;
    requires java.net.http;

    exports com.regor.desktop;
    opens com.regor.desktop.controller to javafx.fxml;
}
