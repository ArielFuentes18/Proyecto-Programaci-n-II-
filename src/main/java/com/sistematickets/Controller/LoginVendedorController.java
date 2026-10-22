package com.sistematickets.Controller;

import javafx.fxml.FXML;

public class LoginVendedorController extends LoginBaseController {

    @Override protected TipoUsuario tipo() { return TipoUsuario.VENDEDOR; }
    @Override protected String nombreRol() { return "Vendedor"; }
    @Override protected String rolEsperado() { return "vendedor"; }

    @FXML
    public void onOlvido() {
        mensaje("Contacta a tu supervisor para restablecer tu contraseña.", false);
    }
}
