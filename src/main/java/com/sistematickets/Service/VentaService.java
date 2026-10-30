package com.sistematickets.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sistematickets.Model.Venta;

import java.util.concurrent.CompletableFuture;

public class VentaService {
    private final ApiClient api = ApiClient.getInstance();

    public CompletableFuture<Venta> registrar(Venta venta) {
        return api.post(Endpoints.VENTAS, venta, new TypeReference<Venta>() {});
    }
}
