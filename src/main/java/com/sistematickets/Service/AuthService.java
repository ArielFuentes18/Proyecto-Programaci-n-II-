package com.sistematickets.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sistematickets.Model.LoginResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AuthService {
    private final ApiClient api = ApiClient.getInstance();

    public CompletableFuture<LoginResponse> login(String correo, String password) {
        return api.post(Endpoints.LOGIN, Map.of("correo", correo, "password", password),
                new TypeReference<LoginResponse>() {});
    }
}
