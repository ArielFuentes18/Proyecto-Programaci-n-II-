package com.sistematickets.Service;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


public final class ImagenService {

    private static final ImagenService INSTANCE = new ImagenService();
    public static ImagenService getInstance() { return INSTANCE; }

    private static final String CLAVE_URL = "imagen.url.solicitada";

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final ConcurrentHashMap<String, CompletableFuture<byte[]>> cache = new ConcurrentHashMap<>();

    private ImagenService() {}

    public static String normalizar(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String u = raw.replaceAll("[\\r\\n\\t]", "").trim();   
        String candidata;
        if (u.startsWith("http://") || u.startsWith("https://")) {
            candidata = u;
        } else {
            String archivo = u.substring(u.lastIndexOf('/') + 1);
            candidata = ApiClient.BASE_URL + AppConfig.EP_ARTISTAS + "/imagen/" + archivo;
        }
        return codificar(candidata);
    }

    private static String codificar(String url) {
        StringBuilder sb = new StringBuilder(url.length() + 16);
        for (char ch : url.toCharArray()) {
            if (ch == ' ') sb.append("%20");
            else if ("\"<>\\^`{|}[]".indexOf(ch) >= 0) sb.append(String.format("%%%02X", (int) ch));
            else sb.append(ch);
        }
        String resultado = sb.toString();
        try {
            URI.create(resultado);
            return resultado;
        } catch (IllegalArgumentException e) {
            System.err.println("[Imagen] URL inválida, se usará el placeholder: " + url);
            return null;
        }
    }

    private CompletableFuture<byte[]> descargar(String url) {
        CompletableFuture<byte[]> existente = cache.get(url);
        if (existente != null) return existente;
        CompletableFuture<byte[]> nuevo;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(60))      
                    .header("Accept", "image/jpeg,image/png,image/gif,*/*")
                    .GET().build();
            nuevo = http.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray())
                    .thenApply(r -> {
                        String tipo = r.headers().firstValue("Content-Type").orElse("?");
                        if (r.statusCode() != 200) {
                            throw new IllegalStateException("HTTP " + r.statusCode() + " al pedir " + url);
                        }
                        if (tipo.contains("webp")) {
                            throw new IllegalStateException("JavaFX no soporta WebP (" + url + "). Sube JPG, PNG o GIF.");
                        }
                        return r.body();
                    });
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
        CompletableFuture<byte[]> previo = cache.putIfAbsent(url, nuevo);
        if (previo != null) return previo;
        nuevo.whenComplete((bytes, ex) -> { if (ex != null) cache.remove(url); });   
        return nuevo;
    }

 
    public void mostrar(ImageView destino, String urlOCruda, Image placeholder, Consumer<Image> alListo) {
        try {
            String url = normalizar(urlOCruda);
            destino.getProperties().put(CLAVE_URL, String.valueOf(url));  
            if (url == null) {
                poner(destino, placeholder, alListo);
                return;
            }
            descargar(url).whenComplete((bytes, ex) -> {
                Image img = null;
                try {
                    if (ex == null) {
                     
                        Image decodificada = new Image(new ByteArrayInputStream(bytes));
                        if (!decodificada.isError() && decodificada.getWidth() > 0) img = decodificada;
                        else System.err.println("[Imagen] No se pudo decodificar " + url + " : " + decodificada.getException());
                    } else {
                        Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                        System.err.println("[Imagen] Falló la descarga: " + causa.getMessage());
                    }
                } catch (RuntimeException e) {
                    System.err.println("[Imagen] Error al procesar " + url + " : " + e);
                }
                final Image resultado = img != null ? img : placeholder;
                Platform.runLater(() -> {
                    if (String.valueOf(url).equals(destino.getProperties().get(CLAVE_URL))) {
                        poner(destino, resultado, alListo);
                    }
                });
            });
        } catch (RuntimeException e) {
            System.err.println("[Imagen] " + e);
            poner(destino, placeholder, alListo);
        }
    }

    private void poner(ImageView destino, Image img, Consumer<Image> alListo) {
        destino.setImage(img);
        if (img != null && alListo != null) alListo.accept(img);
    }
}
