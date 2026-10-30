package com.sistematickets.Controller;

import com.sistematickets.Model.Usuario;
import com.sistematickets.Service.AuthService;

import java.util.concurrent.CompletableFuture;

public class AuthController {
    private final AuthService authService = new AuthService();

    public CompletableFuture<Usuario> login(String correo, String password) {
        if (correo == null || correo.isBlank() || password == null || password.isBlank()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Correo y contraseña son obligatorios"));
        }
        return authService.login(correo.trim(), password).thenApply(resp -> {
            if (resp == null || resp.usuario() == null) {
                throw new IllegalStateException("Respuesta de login inválida");
            }
            Sesion.iniciar(resp.usuario(), resp.token());
            return resp.usuario();
        });
    }

    public void logout() { Sesion.cerrar(); }
}
