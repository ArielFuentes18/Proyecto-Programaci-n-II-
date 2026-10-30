package com.sistematickets.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sistematickets.Model.InventarioBoletos;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class InventarioService {
    private final ApiClient api = ApiClient.getInstance();

    public CompletableFuture<List<InventarioBoletos>> porConcierto(int idConcierto) {
        return api.get(Endpoints.INVENTARIO_POR_CONCIERTO + idConcierto,
                new TypeReference<List<InventarioBoletos>>() {});
    }

    public CompletableFuture<List<InventarioBoletos>> todos() {
        return api.get(Endpoints.INVENTARIO, new TypeReference<List<InventarioBoletos>>() {});
    }
}
