module com.emlinha {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.emlinha to javafx.fxml;
    exports com.emlinha;
}
