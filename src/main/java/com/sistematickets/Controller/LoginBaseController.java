package com.sistematickets.Controller;

import com.sistematickets.Model.Usuario;
import com.sistematickets.Service.ApiException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/** Lógica común de los tres logins. Cada rol tiene su propio FXML y su subclase. */
public abstract class LoginBaseController {

    @FXML protected TextField tfUsuario;
    @FXML protected PasswordField pfPassword;
    @FXML protected TextField tfPassword;
    @FXML protected Button btnEye;
    @FXML protected Button btnLogin;
    @FXML protected Label lblErrUsuario;
    @FXML protected Label lblErrPassword;
    @FXML protected Label lblMensaje;

    private final AuthController auth = new AuthController();
    private Runnable onVolver;
    private Consumer<Usuario> onExito;

    protected abstract TipoUsuario tipo();
    protected abstract String nombreRol();
    /** Nombre del rol en la base de datos; null si no se valida (cliente). */
    protected abstract String rolEsperado();

    public void setOnVolver(Runnable r) { this.onVolver = r; }
    public void setOnExito(Consumer<Usuario> c) { this.onExito = c; }

    @FXML
    public void initialize() {
        tfPassword.textProperty().bindBidirectional(pfPassword.textProperty());
        tfUsuario.textProperty().addListener((o, a, b) -> limpiar(lblErrUsuario, tfUsuario));
        pfPassword.textProperty().addListener((o, a, b) -> limpiar(lblErrPassword, pfPassword, tfPassword));
    }

    @FXML
    public void onVolver() {
        if (onVolver != null) onVolver.run();
    }

    @FXML
    public void onToggleEye() {
        boolean ver = !tfPassword.isVisible();
        tfPassword.setVisible(ver);
        tfPassword.setManaged(ver);
        pfPassword.setVisible(!ver);
        pfPassword.setManaged(!ver);
        btnEye.setText(ver ? "Ocultar" : "Mostrar");
        (ver ? tfPassword : pfPassword).requestFocus();
    }

    @FXML
    public void onLogin() {
        String usuario = tfUsuario.getText().trim();
        String pass = pfPassword.getText();
        boolean ok = true;
        if (usuario.isEmpty()) {
            error(lblErrUsuario, "Escribe tu usuario o correo.", tfUsuario);
            ok = false;
        }
        if (pass.length() < 6) {
            error(lblErrPassword, "La contraseña debe tener al menos 6 caracteres.", pfPassword, tfPassword);
            ok = false;
        }
        if (!ok) return;

        cargando(true);
        mensaje(null, false);
        auth.login(usuario, pass).whenComplete((u, ex) -> Platform.runLater(() -> {
            cargando(false);
            if (ex != null) {
                mensaje(textoError(ex), true);
                return;
            }
            if (!rolPermitido(u)) {
                Sesion.cerrar();
                mensaje("Esta cuenta no tiene acceso como " + nombreRol() + ".", true);
                return;
            }
            Sesion.tipoSeleccionado(tipo());
            mensaje("Sesión iniciada. Bienvenido, " + (u.nombre() == null ? "" : u.nombre()) + ".", false);
            if (onExito != null) onExito.accept(u);
        }));
    }

    // ---------- utilidades ----------
    private boolean rolPermitido(Usuario u) {
        String esperado = rolEsperado();
        if (esperado == null || u == null || u.rol() == null) return true;
        return esperado.equalsIgnoreCase(u.rol());
    }

    private static String textoError(Throwable t) {
        while (t instanceof CompletionException && t.getCause() != null) t = t.getCause();
        if (t instanceof ApiException ae && (ae.getStatus() == 400 || ae.getStatus() == 401 || ae.getStatus() == 403)) {
            return "Credenciales incorrectas. Verifica tu usuario y contraseña.";
        }
        return t.getMessage() != null ? t.getMessage() : "No se pudo iniciar sesión.";
    }

    private void cargando(boolean on) {
        btnLogin.setDisable(on);
        btnLogin.setText(on ? "Verificando…" : "Iniciar sesión");
        tfUsuario.setDisable(on);
        pfPassword.setDisable(on);
        tfPassword.setDisable(on);
    }

    protected void mensaje(String texto, boolean esError) {
        boolean visible = texto != null && !texto.isEmpty();
        lblMensaje.setText(visible ? texto : "");
        lblMensaje.setVisible(visible);
        lblMensaje.setManaged(visible);
        lblMensaje.getStyleClass().removeAll("msg-error", "msg-ok");
        lblMensaje.getStyleClass().add(esError ? "msg-error" : "msg-ok");
    }

    private void error(Label etiqueta, String texto, Node... campos) {
        etiqueta.setText(texto);
        etiqueta.setVisible(true);
        etiqueta.setManaged(true);
        for (Node n : campos) if (!n.getStyleClass().contains("invalid")) n.getStyleClass().add("invalid");
    }

    private void limpiar(Label etiqueta, Node... campos) {
        etiqueta.setVisible(false);
        etiqueta.setManaged(false);
        for (Node n : campos) n.getStyleClass().remove("invalid");
    }
}
