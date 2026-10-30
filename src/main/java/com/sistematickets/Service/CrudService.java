package com.sistematickets.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


public class CrudService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<Map<String, Object>>> LISTA = new TypeReference<List<Map<String, Object>>>() {};
    private static final TypeReference<JsonNode> NODO = new TypeReference<JsonNode>() {};

    private final ApiClient api = ApiClient.getInstance();

    public CompletableFuture<List<Map<String, Object>>> listar(String endpoint) {
        return api.get(endpoint, NODO).thenApply(n -> {
            if (n == null || n.isNull()) return List.<Map<String, Object>>of();
            JsonNode arr = n.isArray() ? n : n.path("data");
            if (!arr.isArray()) return List.<Map<String, Object>>of();
            return MAPPER.convertValue(arr, LISTA);
        });
    }


    public CompletableFuture<JsonNode> enviar(String ruta, Map<String, Object> cuerpo) {
        return api.post(ruta, cuerpo, NODO);
    }

    public CompletableFuture<JsonNode> crear(String endpoint, Map<String, Object> cuerpo) {
        return api.post(endpoint + "/create", cuerpo, NODO);
    }

    public CompletableFuture<JsonNode> actualizar(String endpoint, Object id, Map<String, Object> cuerpo) {
        return api.put(endpoint + "/update/" + id, cuerpo, NODO);
    }

    public CompletableFuture<JsonNode> eliminar(String endpoint, Object id) {
        return api.delete(endpoint + "/delete/" + id, NODO);
    }
}
