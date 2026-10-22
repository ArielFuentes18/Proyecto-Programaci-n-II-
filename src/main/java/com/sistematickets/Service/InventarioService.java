package com.sistematickets.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sistematickets.Model.InventarioBoletos;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class InventarioService {
    private final ApiClient api = ApiClient.getInstance();

    /** Consulta en tiempo real (sin caché) el inventario de un concierto. */
    public CompletableFuture<List<InventarioBoletos>> porConcierto(int idConcierto) {
        return api.get(Endpoints.INVENTARIO_POR_CONCIERTO + idConcierto,
                new TypeReference<List<InventarioBoletos>>() {});
    }
}
