package com.sistematickets.Controller;

import com.sistematickets.Service.AppConfig;
import com.sistematickets.Service.CrudService;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

public class LoginAdminController extends LoginBaseController {

    @FXML private StackPane modal;
    @FXML private TextField tfRegNombre;
    @FXML private TextField tfRegCorreo;
    @FXML private PasswordField pfRegPass;
    @FXML private Label lblRegMsg;
    @FXML private Button btnRegEnviar;

    @Override protected TipoUsuario tipo() { return TipoUsuario.ADMIN; }
    @Override protected String nombreRol() { return "Administrador"; }
    @Override protected String rolEsperado() { return "administrador"; }

    @FXML
    @Override
    public void initialize() {
        super.initialize();
        modal.setOnMouseClicked(e -> { if (e.getTarget() == modal) onCerrarRegistro(); });
    }

    @FXML
    public void onAbrirRegistro() {
        msgRegistro(null);
        modal.setOpacity(0);
        modal.setManaged(true);
        modal.setVisible(true);
        FadeTransition f = new FadeTransition(Duration.millis(220), modal);
        f.setToValue(1);
        f.play();
    }

    @FXML
    public void onCerrarRegistro() {
        FadeTransition f = new FadeTransition(Duration.millis(180), modal);
        f.setToValue(0);
        f.setOnFinished(e -> { modal.setVisible(false); modal.setManaged(false); });
        f.play();
    }


    @FXML
    public void onEnviarRegistro() {
        String nombre = tfRegNombre.getText().trim();
        String correo = tfRegCorreo.getText().trim();
        String pass = pfRegPass.getText();
        if (nombre.isEmpty() || !correo.matches("^\\S+@\\S+\\.\\S+$") || pass.length() != 6) {
            msgRegistro("Completa todos los campos (correo válido y contraseña de 6 dígitos).");
            return;
        }
        msgRegistro(null);
        btnRegEnviar.setDisable(true);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nombre", nombre);
        body.put("correo", correo);
        body.put("password", pass);
        new CrudService().enviar(AppConfig.EP_USUARIOS + "/solicitar-admin", body).whenComplete((r, ex) -> Platform.runLater(() -> {
            btnRegEnviar.setDisable(false);
            if (ex != null) {
                Throwable t = ex;
                while (t instanceof CompletionException && t.getCause() != null) t = t.getCause();
                msgRegistro(t.getMessage() != null ? t.getMessage() : "No se pudo enviar la solicitud.");
                return;
            }
            tfRegNombre.clear();
            tfRegCorreo.clear();
            pfRegPass.clear();
            onCerrarRegistro();
            mensaje("Solicitud enviada. Un administrador debe aprobarla.", false);
        }));
    }

    private void msgRegistro(String texto) {
        boolean v = texto != null && !texto.isEmpty();
        lblRegMsg.setText(v ? texto : "");
        lblRegMsg.setVisible(v);
        lblRegMsg.setManaged(v);
    }
}
