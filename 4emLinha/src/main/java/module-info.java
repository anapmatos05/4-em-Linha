module com.emlinha {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics; // Necessário para o Canvas e GraphicsContext funcionarem

    opens com.emlinha to javafx.fxml;
    exports com.emlinha;
}