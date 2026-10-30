package com.sistematickets.Controller;

import javafx.fxml.FXML;

import java.io.UncheckedIOException;

public class LoginClienteController extends LoginBaseController {

    @Override protected TipoUsuario tipo() { return TipoUsuario.CLIENTE; }
    @Override protected String nombreRol() { return "Cliente"; }

    @Override protected String rolEsperado() { return "cliente"; }

    @FXML
    public void onCrearCuenta() {
        try {
            ClienteUI.ir("registro-cliente");
        } catch (UncheckedIOException ex) {
            mensaje("No se pudo abrir el registro: " + ex.getMessage(), true);
        }
    }
}
