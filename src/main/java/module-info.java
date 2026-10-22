module com.sistematickets {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.media;
    requires javafx.base;
    requires java.sql;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires org.postgresql.jdbc;

    opens com.sistematickets to javafx.fxml;
    opens com.sistematickets.Controller to javafx.fxml;
    opens com.sistematickets.Model to com.fasterxml.jackson.databind;

    exports com.sistematickets;
    exports com.sistematickets.Model;
    exports com.sistematickets.Service;
    exports com.sistematickets.Controller;
    exports conexion;
}
