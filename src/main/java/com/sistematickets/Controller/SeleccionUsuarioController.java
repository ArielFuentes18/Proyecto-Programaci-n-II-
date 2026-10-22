package com.sistematickets.Controller;

import com.sistematickets.App;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;

/** Pantalla "Selecciona tipo de usuario" con video de fondo. */
public class SeleccionUsuarioController {

    /** Ruta del video dentro de src/main/resources. */
    private static final String RUTA_VIDEO = "/com/sistematickets/video/fondo.mp4";

    @FXML private StackPane root;
    @FXML private MediaView mediaView;
    @FXML private VBox content;
    @FXML private Button btnVendedor;
    @FXML private Button btnAdmin;
    @FXML private Button btnCliente;
    @FXML private Label lblEstado;

    private MediaPlayer player;

    @FXML
    private void initialize() {
        configurarVideo();
        for (Button b : new Button[]{btnVendedor, btnAdmin, btnCliente}) {
            animarHover(b);
        }
        animarEntrada();
    }

    // ---------- Navegación entre selector y logins ----------
    @FXML private void onVendedor() { abrirLogin(TipoUsuario.VENDEDOR); }
    @FXML private void onAdmin()    { abrirLogin(TipoUsuario.ADMIN); }
    @FXML private void onCliente()  { abrirLogin(TipoUsuario.CLIENTE); }

    private boolean animando;

    private void abrirLogin(TipoUsuario tipo) {
        if (animando) return;
        String vista = switch (tipo) {
            case ADMIN -> "login-admin";
            case VENDEDOR -> "login-vendedor";
            case CLIENTE -> "login-cliente";
        };
        try {
            FXMLLoader loader = new FXMLLoader(
                    SeleccionUsuarioController.class.getResource("/com/sistematickets/" + vista + ".fxml"));
            Parent login = loader.load();
            LoginBaseController ctrl = loader.getController();
            ctrl.setOnVolver(() -> cambiarVista(login, content));
            ctrl.setOnExito(usuario -> {
                if (tipo == TipoUsuario.ADMIN) {
                    detener();
                    try {
                        App.setRoot("panel-admin");
                    } catch (IOException ex) {
                        lblEstado.setText("No se pudo abrir el panel: " + ex.getMessage());
                    }
                }
                // TODO: paneles de Vendedor y Cliente
            });
            cambiarVista(content, login);
        } catch (IOException e) {
            lblEstado.setText("No se pudo abrir el acceso: " + e.getMessage());
        }
    }

    /** Fundido de salida de la vista actual y fundido de entrada de la nueva. */
    private void cambiarVista(Node actual, Node nueva) {
        if (animando) return;
        animando = true;
        FadeTransition salida = new FadeTransition(Duration.millis(240), actual);
        salida.setToValue(0);
        salida.setOnFinished(e -> {
            root.getChildren().remove(actual);
            actual.setOpacity(1);
            nueva.setOpacity(0);
            root.getChildren().add(nueva);
            FadeTransition entrada = new FadeTransition(Duration.millis(320), nueva);
            entrada.setToValue(1);
            TranslateTransition sube = new TranslateTransition(Duration.millis(320), nueva);
            sube.setFromY(16);
            sube.setToY(0);
            ParallelTransition par = new ParallelTransition(entrada, sube);
            par.setOnFinished(ev -> animando = false);
            par.play();
        });
        salida.play();
    }

    // ---------- Video de fondo ----------
    private void configurarVideo() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());
        root.setClip(clip);

        URL url = SeleccionUsuarioController.class.getResource(RUTA_VIDEO);
        if (url == null) {
            System.err.println("[Video] No se encontro " + RUTA_VIDEO + " -> se usa el fondo degradado.");
            ocultarVideo();
            return;
        }
        try {
            Media media = new Media(url.toExternalForm());
            player = new MediaPlayer(media);
            player.setMute(true);
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setAutoPlay(true);
            player.setOnError(() -> {
                System.err.println("[Video] Error: " + player.getError());
                ocultarVideo();
            });
            media.setOnError(() -> {
                System.err.println("[Video] Error: " + media.getError());
                ocultarVideo();
            });
            player.setOnReady(this::ajustarCover);
            mediaView.setMediaPlayer(player);
            mediaView.setPreserveRatio(true);
            root.widthProperty().addListener((o, a, b) -> ajustarCover());
            root.heightProperty().addListener((o, a, b) -> ajustarCover());
        } catch (Exception e) {
            System.err.println("[Video] No se pudo cargar: " + e.getMessage());
            ocultarVideo();
        }
    }

    /** Escala el video para cubrir toda la ventana (efecto "cover"). */
    private void ajustarCover() {
        if (player == null) return;
        double vw = player.getMedia().getWidth();
        double vh = player.getMedia().getHeight();
        double w = root.getWidth();
        double h = root.getHeight();
        if (vw <= 0 || vh <= 0 || w <= 0 || h <= 0) return;
        double escala = Math.max(w / vw, h / vh);
        mediaView.setFitWidth(vw * escala);
        mediaView.setFitHeight(vh * escala);
    }

    private void ocultarVideo() {
        mediaView.setVisible(false);
        mediaView.setManaged(false);
    }

    /** Libera el video al cerrar la ventana. */
    public void detener() {
        if (player != null) {
            player.stop();
            player.dispose();
            player = null;
        }
    }

    // ---------- Animaciones ----------
    private void animarEntrada() {
        content.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(800), content);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(800), content);
        slide.setFromY(24);
        slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }

    private void animarHover(Button b) {
        ScaleTransition in = new ScaleTransition(Duration.millis(160), b);
        in.setToX(1.06);
        in.setToY(1.06);
        ScaleTransition out = new ScaleTransition(Duration.millis(160), b);
        out.setToX(1.0);
        out.setToY(1.0);
        b.setOnMouseEntered(e -> { out.stop(); in.playFromStart(); });
        b.setOnMouseExited(e -> { in.stop(); out.playFromStart(); });
    }
}
