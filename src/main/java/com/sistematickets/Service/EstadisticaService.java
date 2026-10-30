package com.sistematickets.Service;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EstadisticaService {
    private final ApiClient api = ApiClient.getInstance();

    public CompletableFuture<Map<String, Object>> resumen(int dias) {
        return api.get(AppConfig.EP_ESTADISTICAS + "/resumen?dias=" + dias,
                new TypeReference<Map<String, Object>>() {});
    }
}
