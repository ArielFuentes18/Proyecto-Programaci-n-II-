package com.sistematickets.Controller;

import com.sistematickets.Service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegistroClienteController {

    @FXML private TextField tfNombre;
    @FXML private TextField tfCorreo;
    @FXML private PasswordField pfPassword;
    @FXML private PasswordField pfConfirmar;
    @FXML private Label errNombre;
    @FXML private Label errCorreo;
    @FXML private Label errPassword;
    @FXML private Label errConfirmar;
    @FXML private Label lblMensaje;
    @FXML private Button btnRegistrar;

    private final AuthService authService = new AuthService();
    private final AuthController auth = new AuthController();

    @FXML
    private void initialize() {
        tfNombre.textProperty().addListener((o, a, b) -> limpiar(errNombre, tfNombre));
        tfCorreo.textProperty().addListener((o, a, b) -> limpiar(errCorreo, tfCorreo));
        pfPassword.textProperty().addListener((o, a, b) -> limpiar(errPassword, pfPassword));
        pfConfirmar.textProperty().addListener((o, a, b) -> limpiar(errConfirmar, pfConfirmar));
    }

    @FXML
    private void onVolver() {
        ClienteUI.ir("seleccion-usuario");
    }

    @FXML
    private void onRegistrar() {
        String nombre = tfNombre.getText().trim();
        String correo = tfCorreo.getText().trim();
        String pass = pfPassword.getText();
        boolean ok = true;
        if (nombre.length() < 3) { error(errNombre, "Escribe tu nombre completo.", tfNombre); ok = false; }
        if (!correo.matches("^\\S+@\\S+\\.\\S+$")) { error(errCorreo, "Escribe un correo válido.", tfCorreo); ok = false; }
        if (pass.length() < 6 || pass.length() > 30) { error(errPassword, "Entre 6 y 30 caracteres.", pfPassword); ok = false; }
        if (!pass.equals(pfConfirmar.getText())) { error(errConfirmar, "Las contraseñas no coinciden.", pfConfirmar); ok = false; }
        if (!ok) return;

        btnRegistrar.setDisable(true);
        btnRegistrar.setText("Creando cuenta…");
        mensaje(null, false);
        authService.registrarCliente(nombre, correo, pass)
                .thenCompose(r -> auth.login(correo, pass))
                .whenComplete((u, ex) -> Platform.runLater(() -> {
                    btnRegistrar.setDisable(false);
                    btnRegistrar.setText("Crear cuenta");
                    if (ex != null) { mensaje(ClienteUI.mensajeDe(ex), true); return; }
                    Sesion.tipoSeleccionado(TipoUsuario.CLIENTE);
                    ClienteUI.ir("cliente-catalogo");
                }));
    }

    private void mensaje(String texto, boolean error) {
        boolean v = texto != null && !texto.isEmpty();
        lblMensaje.setText(v ? texto : "");
        lblMensaje.setVisible(v);
        lblMensaje.setManaged(v);
        lblMensaje.getStyleClass().removeAll("msg-error", "msg-ok");
        lblMensaje.getStyleClass().add(error ? "msg-error" : "msg-ok");
    }

    private void error(Label etiqueta, String texto, javafx.scene.control.Control campo) {
        etiqueta.setText(texto);
        etiqueta.setVisible(true);
        etiqueta.setManaged(true);
        if (!campo.getStyleClass().contains("invalid")) campo.getStyleClass().add("invalid");
    }

    private void limpiar(Label etiqueta, javafx.scene.control.Control campo) {
        etiqueta.setVisible(false);
        etiqueta.setManaged(false);
        campo.getStyleClass().remove("invalid");
    }
}
