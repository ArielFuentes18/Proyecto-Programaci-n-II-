module com.sistematickets {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;

    // Si usas PostgreSQL
    requires java.sql;

    opens com.sistematickets to javafx.fxml;
    exports com.sistematickets;
}

