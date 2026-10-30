package com.sistematickets;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("seleccion-usuario"), 1100, 720);
        for (String css : new String[]{"seleccion-usuario", "menuvendedor", "estilos-compra", "cliente", "tema"}) {
            scene.getStylesheets().add(App.class.getResource("css/" + css + ".css").toExternalForm());
        }
        stage.setTitle("Sistema de Venta de Tickets");
        stage.setMinWidth(860);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.setMaximized(true);          
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    public static void asegurarTamanio(double ancho, double alto) {
        if (scene == null || !(scene.getWindow() instanceof Stage st)) return;
        if (st.isMaximized() || st.isFullScreen()) return;        
        boolean cambio = false;
        if (st.getWidth() < ancho) { st.setWidth(ancho); cambio = true; }
        if (st.getHeight() < alto) { st.setHeight(alto); cambio = true; }
        if (cambio) st.centerOnScreen();
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }

}