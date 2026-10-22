package com.sistematickets.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Sube imágenes al bucket de Supabase Storage y devuelve su URL pública. */
public class StorageService {

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public CompletableFuture<String> subir(File archivo) {
        return CompletableFuture.supplyAsync(() -> {
            if (AppConfig.SUPA_URL.contains("TU-PROYECTO") || AppConfig.SUPA_KEY.startsWith("TU_")) {
                throw new IllegalStateException("Configura SUPA_URL y SUPA_KEY en AppConfig.java.");
            }
            if (archivo.length() > 5L * 1024 * 1024) {
                throw new IllegalStateException("La imagen no debe superar 5 MB.");
            }
            String nombre = archivo.getName().toLowerCase();
            int punto = nombre.lastIndexOf('.');
            String ext = punto >= 0 ? nombre.substring(punto + 1) : "";
            String mime = switch (ext) {
                case "jpg", "jpeg" -> "image/jpeg";
                case "png" -> "image/png";
                case "webp" -> "image/webp";
                case "gif" -> "image/gif";
                default -> throw new IllegalStateException("Formato no permitido. Usa JPG, PNG, WebP o GIF.");
            };
            String ruta = UUID.randomUUID() + "." + ext;
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(
                                AppConfig.SUPA_URL + "/storage/v1/object/" + AppConfig.BUCKET + "/" + ruta))
                        .timeout(Duration.ofSeconds(60))
                        .header("apikey", AppConfig.SUPA_KEY)
                        .header("Authorization", "Bearer " + AppConfig.SUPA_KEY)
                        .header("Content-Type", mime)
                        .POST(HttpRequest.BodyPublishers.ofFile(archivo.toPath()))
                        .build();
                HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() < 200 || r.statusCode() >= 300) {
                    String msg;
                    try {
                        msg = mapper.readTree(r.body()).path("message").asText("");
                    } catch (Exception e) {
                        msg = "";
                    }
                    throw new IllegalStateException(msg.isEmpty()
                            ? "No se pudo subir la imagen (HTTP " + r.statusCode() + "). Revisa las políticas del bucket."
                            : "No se pudo subir la imagen: " + msg);
                }
                return AppConfig.SUPA_URL + "/storage/v1/object/public/" + AppConfig.BUCKET + "/" + ruta;
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo conectar con Supabase Storage.", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Subida cancelada.", e);
            }
        });
    }
}
