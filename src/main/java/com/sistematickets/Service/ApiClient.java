package com.sistematickets.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Cliente HTTP singleton, asíncrono, con Bearer token y 1 reintento (cold start de Render). */
public final class ApiClient {

    public static final String BASE_URL = "https://backend-concierto-jlva.onrender.com";
    private static final ApiClient INSTANCE = new ApiClient();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private volatile String token;

    private ApiClient() {}

    public static ApiClient getInstance() { return INSTANCE; }

    public void setToken(String token) { this.token = token; }

    public <T> CompletableFuture<T> get(String path, TypeReference<T> type) {
        return attempt(builder(path).GET().build(), type, 1);
    }

    public <T> CompletableFuture<T> post(String path, Object body, TypeReference<T> type) {
        String json;
        try {
            json = mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ApiException("No se pudo serializar la solicitud", e));
        }
        return attempt(builder(path).POST(HttpRequest.BodyPublishers.ofString(json)).build(), type, 1);
    }

    public <T> CompletableFuture<T> put(String path, Object body, TypeReference<T> type) {
        String json;
        try {
            json = mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(new ApiException("No se pudo serializar la solicitud", e));
        }
        return attempt(builder(path).PUT(HttpRequest.BodyPublishers.ofString(json)).build(), type, 1);
    }

    public <T> CompletableFuture<T> delete(String path, TypeReference<T> type) {
        return attempt(builder(path).DELETE().build(), type, 1);
    }

    private HttpRequest.Builder builder(String path) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
        String t = token;
        if (t != null && !t.isBlank()) b.header("Authorization", "Bearer " + t);
        return b;
    }

    private <T> CompletableFuture<T> attempt(HttpRequest req, TypeReference<T> type, int retriesLeft) {
        return http.sendAsync(req, BodyHandlers.ofString())
                .thenApply(r -> parse(r, type))
                .exceptionallyCompose(ex -> {
                    Throwable cause = unwrap(ex);
                    if (retriesLeft > 0 && cause instanceof IOException) {
                        return attempt(req, type, retriesLeft - 1);
                    }
                    if (cause instanceof ApiException) return CompletableFuture.<T>failedFuture(cause);
                    return CompletableFuture.<T>failedFuture(
                            new ApiException("Error de conexión con el servidor: " + cause.getMessage(), cause));
                });
    }

    private <T> T parse(HttpResponse<String> r, TypeReference<T> type) {
        String body = r.body();
        int status = r.statusCode();
        if (status < 200 || status >= 300) throw new ApiException(status, extractMessage(body, status));
        if (body == null || body.isBlank()) return null;
        try {
            return mapper.readValue(body, type);
        } catch (JsonProcessingException e) {
            throw new ApiException("Respuesta inválida del servidor: " + e.getOriginalMessage(), e);
        }
    }

    private String extractMessage(String body, int status) {
        try {
            JsonNode n = mapper.readTree(body);
            for (String k : new String[]{"message", "mensaje", "error", "detail"}) {
                if (n.hasNonNull(k)) return n.get(k).asText();
            }
        } catch (Exception ignored) { }
        return "HTTP " + status;
    }

    private static Throwable unwrap(Throwable t) {
        while (t instanceof CompletionException && t.getCause() != null) t = t.getCause();
        return t;
    }
}
