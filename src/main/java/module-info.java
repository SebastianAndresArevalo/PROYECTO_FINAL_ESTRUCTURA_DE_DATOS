module com.example.fintech {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.example.fintech to javafx.fxml;

    exports modelo;
    exports estructuras;
    exports servicio;
}