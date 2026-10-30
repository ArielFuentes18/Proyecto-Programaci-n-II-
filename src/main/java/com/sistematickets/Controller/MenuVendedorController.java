package com.sistematickets.Controller;

import com.sistematickets.Model.Concierto;
import com.sistematickets.Model.Usuario;
import com.sistematickets.Service.ApiException;
import com.sistematickets.Service.ConciertoService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletionException;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controlador del FXML del MENÚ (BorderPane con barra superior, ScrollPane y FlowPane).
 * Su trabajo: obtener los conciertos, crear una tarjeta por cada uno y agregarlas al FlowPane.
 * Cada tarjeta se llena con TarjetaConciertoController.
 */
public class MenuVendedorController {

    // Ruta del FXML de la tarjeta dentro de resources.
    private static final String FXML_TARJETA = "/com/sistematickets/tarjeta-concierto.fxml";
// Ruta del FXML del login del vendedor. Cámbiala por el nombre real de tu archivo.
private static final String FXML_LOGIN = "/com/sistematickets/seleccion-usuario.fxml";
    // ---- fx:id en Scene Builder (Code -> fx:id) ----
    @FXML private FlowPane contenedorConciertos;          // el FlowPane que está dentro del ScrollPane
    @FXML private ScrollPane scrollConciertos;            // opcional

    @FXML private Label lblSaludo;
@FXML private Label lblBienvenida;

    private final ConciertoService conciertoService = new ConciertoService();
    private final List<Concierto> todos = new ArrayList<>();
@FXML
private void salir() {
    try {
        var url = getClass().getResource(FXML_LOGIN);
        if (url == null) {
            mostrarMensaje("No se encontró el archivo: " + FXML_LOGIN);
            return;
        }
        Sesion.cerrar();                                   // borra el usuario y el token
        Parent root = FXMLLoader.load(url);
        Stage stage = (Stage) contenedorConciertos.getScene().getWindow();
        stage.getScene().setRoot(root);
    } catch (IOException ex) {
        ex.printStackTrace();
        mostrarMensaje("No se pudo volver al login: " + ex.getMessage());
    }
}
    @FXML
    private void initialize() {
        if (scrollConciertos != null) {
            scrollConciertos.setFitToWidth(true);   // las tarjetas pasan a la siguiente fila
        }
        cargarConciertos();
        lblSaludo.setText("Hola, " + nombreUsuario());
    }
private String nombreUsuario() {
    Usuario u = Sesion.usuario();
    if (u != null && u.nombre() != null && !u.nombre().isBlank()) {
        String n = u.nombre().trim();
        return n.contains(" ") ? n.substring(0, n.indexOf(' ')) : n;   // solo el primer nombre
    }
    return "Vendedor";
}
    // ---------- Carga de datos ----------

    /** Pide los conciertos al backend en un hilo aparte para no congelar la ventana. */
    private void cargarConciertos() {
        mostrarMensaje("Cargando conciertos…");   // el servidor gratuito puede tardar en despertar

        Task<List<Concierto>> tarea = new Task<>() {
            @Override
            protected List<Concierto> call() {
                return obtenerConciertos();
            }
        };
        tarea.setOnSucceeded(e -> {
            todos.clear();
            todos.addAll(tarea.getValue());
            mostrarConciertos(todos);
        });
        tarea.setOnFailed(e -> {
            Throwable ex = tarea.getException();
            while (ex instanceof CompletionException && ex.getCause() != null) ex = ex.getCause();
            ex.printStackTrace();
            if (ex instanceof ApiException ae && (ae.getStatus() == 401 || ae.getStatus() == 403)) {
                mostrarMensaje("No tienes permiso para ver los conciertos. Inicia sesión de nuevo.");
            } else {
                mostrarMensaje("No se pudieron cargar los conciertos: " + ex.getMessage());
            }
        });

        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Se ejecuta en un hilo aparte, así que puede esperar la respuesta del backend. */
    private List<Concierto> obtenerConciertos() {
        List<Concierto> lista = conciertoService.listar().join();
        if (lista == null) return List.of();

        List<Concierto> ordenada = new ArrayList<>(lista);
        // Las fechas ISO se ordenan bien como texto: primero los más próximos
        ordenada.sort(Comparator.comparing(Concierto::fechaConcierto,
                Comparator.nullsLast(Comparator.naturalOrder())));

        // Diagnóstico: revisa en la consola qué está llegando. Bórralo cuando todo funcione.
        for (Concierto c : ordenada) {
            System.out.println("[Menú] concierto " + c.idConcierto()
                    + " | artista='" + c.nombreArtista() + "'"
                    + " | imagen=" + c.imagenPath()
                    + " | fecha=" + c.fechaConcierto()
                    + " | recinto=" + c.recinto());
            System.out.println("[Menú] RAW nombre_artistico = " + c.nombreArtistico());
System.out.println("[Menú] RAW artista        = " + c.artistaNode());
System.out.println("[Menú] id_artista         = " + c.idArtista());
        }
        return ordenada;
    }

    // ---------- Pintar las tarjetas ----------

    private void mostrarConciertos(List<Concierto> lista) {
        contenedorConciertos.getChildren().clear();

        if (lista.isEmpty()) {
            mostrarMensaje("No hay conciertos disponibles por ahora.");
            return;
        }

        for (Concierto c : lista) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(FXML_TARJETA));
                VBox tarjeta = loader.load();
                TarjetaConciertoController ctrl = loader.getController();
                ctrl.setDatos(c, this::abrirVenta);
                contenedorConciertos.getChildren().add(tarjeta);
            } catch (IOException | RuntimeException ex) {
                // una tarjeta defectuosa no debe dejar el menú vacío
                System.err.println("[Menú] No se pudo crear la tarjeta del concierto " + c.idConcierto() + ": " + ex);
                ex.printStackTrace();
            }
        }
    }

    private void mostrarMensaje(String texto) {
        Label lbl = new Label(texto);
        lbl.getStyleClass().add("tarjeta-lugar");
        contenedorConciertos.getChildren().setAll(lbl);
    }

    // ---------- Acciones ----------

    /** Se ejecuta al hacer clic en la foto de una tarjeta: abre la selección de boletos (misma vista que el cliente). */
private void abrirVenta(Concierto c) {
    try {
        FlujoCompra.iniciar(c);
        ClienteUI.ir("cliente-boletos");
    } catch (RuntimeException ex) {
        ex.printStackTrace();
        mostrarMensaje("No se pudo abrir la pantalla de compra: " + ex.getMessage());
    }
}

    /**
     * Para los ToggleButton de la barra (On Action = filtrarCategoria).
     * Todavía no filtra: el concierto no tiene una columna de categoría.
     */
    @FXML
    private void filtrarCategoria() {
        mostrarConciertos(todos);
    }
}