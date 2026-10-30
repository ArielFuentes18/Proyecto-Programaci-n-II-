package com.sistematickets.Controller;

import com.sistematickets.Model.Usuario;
import java.io.IOException;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class LoginVendedorController extends LoginBaseController {


    private static final String FXML_MENU = "/com/sistematickets/interfazmenuVendedor.fxml";

    @Override protected TipoUsuario tipo() { return TipoUsuario.VENDEDOR; }
    @Override protected String nombreRol() { return "Vendedor"; }
    @Override protected String rolEsperado() { return "vendedor"; }

    @FXML
    public void onOlvido() {
        mensaje("Contacta a tu supervisor para restablecer tu contraseña.", false);
    }

 

    @Override
    public void initialize() {
        super.initialize();
        super.setOnExito(this::abrirMenu);   
    }


    @Override
    public void setOnExito(Consumer<Usuario> ignorado) {
        super.setOnExito(this::abrirMenu);
    }

    private void abrirMenu(Usuario u) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(FXML_MENU));
            Parent root = loader.load();

            Stage stage = (Stage) btnLogin.getScene().getWindow();
            stage.getScene().setRoot(root);   
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            mensaje("No se pudo abrir el menú del vendedor.", true);
        }
    }
}