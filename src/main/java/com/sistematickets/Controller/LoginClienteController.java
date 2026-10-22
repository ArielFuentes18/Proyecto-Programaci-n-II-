package com.sistematickets.Controller;

import javafx.fxml.FXML;

public class LoginClienteController extends LoginBaseController {

    @Override protected TipoUsuario tipo() { return TipoUsuario.CLIENTE; }
    @Override protected String nombreRol() { return "Cliente"; }
    /** La base de datos no tiene rol "cliente", por eso no se valida el rol. */
    @Override protected String rolEsperado() { return null; }

    @FXML
    public void onCrearCuenta() {
        mensaje("El registro de clientes estará disponible pronto.", false);
    }
}
