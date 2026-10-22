package com.sistematickets.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sistematickets.Model.Venta;

import java.util.concurrent.CompletableFuture;

public class VentaService {
    private final ApiClient api = ApiClient.getInstance();

    /** Envía venta + detalles en un solo POST para que el backend la procese en una transacción. */
    public CompletableFuture<Venta> registrar(Venta venta) {
        return api.post(Endpoints.VENTAS, venta, new TypeReference<Venta>() {});
    }
}
