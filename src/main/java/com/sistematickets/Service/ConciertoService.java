package com.sistematickets.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sistematickets.Model.Concierto;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ConciertoService {
    private final ApiClient api = ApiClient.getInstance();

    public CompletableFuture<List<Concierto>> listar() {
        return api.get(Endpoints.CONCIERTOS, new TypeReference<List<Concierto>>() {});
    }

    public CompletableFuture<Concierto> obtener(int idConcierto) {
        return api.get(Endpoints.CONCIERTOS + "/" + idConcierto, new TypeReference<Concierto>() {});
    }
}
