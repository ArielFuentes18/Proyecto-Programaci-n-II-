package com.sistematickets.Controller;

import com.sistematickets.Model.Usuario;
import com.sistematickets.Service.ApiClient;

public final class Sesion {
    private static volatile Usuario usuario;
    private static volatile TipoUsuario tipoSeleccionado;

    private Sesion() {}

    public static void iniciar(Usuario u, String token) {
        usuario = u;
        ApiClient.getInstance().setToken(token);
    }

    public static void cerrar() {
        usuario = null;
        ApiClient.getInstance().setToken(null);
    }

    public static Usuario usuario() { return usuario; }
    public static TipoUsuario tipoSeleccionado() { return tipoSeleccionado; }
    public static void tipoSeleccionado(TipoUsuario t) { tipoSeleccionado = t; }
    public static boolean activa() { return usuario != null; }
    public static boolean esAdministrador() {
        return usuario != null && "administrador".equalsIgnoreCase(usuario.rol());
    }
}
