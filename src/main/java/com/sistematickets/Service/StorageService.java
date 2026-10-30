package com.sistematickets.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class StorageService {

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public CompletableFuture<String> subir(File archivo) {
        return CompletableFuture.supplyAsync(() -> {
            if (archivo.length() > 5L * 1024 * 1024) {
                throw new IllegalStateException("La imagen no debe superar 5 MB.");
            }
            String nombre = archivo.getName().toLowerCase();
            int punto = nombre.lastIndexOf('.');
            String ext = punto >= 0 ? nombre.substring(punto + 1) : "";
            String mime = switch (ext) {
                case "jpg", "jpeg" -> "image/jpeg";
                case "png" -> "image/png";
                case "gif" -> "image/gif";
                default -> throw new IllegalStateException("Formato no permitido. Usa JPG, PNG o GIF (JavaFX no muestra WebP).");
            };
            try {
                HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(ApiClient.BASE_URL + AppConfig.EP_SUBIR_IMAGEN))
                        .timeout(Duration.ofSeconds(90))
                        .header("Content-Type", mime)
                        .POST(HttpRequest.BodyPublishers.ofFile(archivo.toPath()));
                String token = ApiClient.getInstance().token();
                if (token != null && !token.isBlank()) b.header("Authorization", "Bearer " + token);

                HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() == 401) {
                    throw new IllegalStateException("Tu sesión expiró. Vuelve a iniciar sesión.");
                }
                String msg = "";
                String url = "";
                try {
                    var json = mapper.readTree(r.body());
                    msg = json.path("message").asText("");
                    url = json.path("url").asText("");
                } catch (Exception ignorado) {
                    // respuesta sin JSON
                }
                if (r.statusCode() < 200 || r.statusCode() >= 300 || url.isEmpty()) {
                    throw new IllegalStateException(msg.isEmpty()
                            ? "No se pudo subir la imagen (HTTP " + r.statusCode() + ")."
                            : msg);
                }
                return url;
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo conectar con el servidor para subir la imagen.", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Subida cancelada.", e);
            }
        });
    }
}
