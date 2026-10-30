package com.sistematickets.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


public final class TasasCambio {
    private TasasCambio() {}

    private static final String URL = "https://open.er-api.com/v6/latest/GTQ";
    private static final long VIGENCIA_MS = 30 * 60 * 1000L;     

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    private static final ObjectMapper JSON = new ObjectMapper();

    private static volatile Map<Moneda, BigDecimal> tasas = referenciales();
    private static volatile boolean enLinea;
    private static volatile String actualizadoEl = "";
    private static volatile long ultimaConsulta;

    private static Map<Moneda, BigDecimal> referenciales() {
        Map<Moneda, BigDecimal> m = new EnumMap<>(Moneda.class);
        for (Moneda mo : Moneda.values()) m.put(mo, mo.tasaReferencial());
        return m;
    }

    public static BigDecimal tasa(Moneda m) {
        return m == Moneda.GTQ ? BigDecimal.ONE : tasas.get(m);
    }

    public static BigDecimal quetzalesPor(Moneda m) {
        return BigDecimal.ONE.divide(tasa(m), 4, RoundingMode.HALF_UP);
    }

    public static BigDecimal convertir(BigDecimal quetzales, Moneda m) {
        if (quetzales == null) return BigDecimal.ZERO.setScale(2);
        return quetzales.multiply(tasa(m)).setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean enLinea() { return enLinea; }

    public static String actualizadoEl() { return actualizadoEl; }

    public static CompletableFuture<Boolean> actualizar() {
        if (enLinea && System.currentTimeMillis() - ultimaConsulta < VIGENCIA_MS) return CompletableFuture.completedFuture(true);
        HttpRequest req = HttpRequest.newBuilder(URI.create(URL)).timeout(Duration.ofSeconds(8)).GET().build();
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenApply(resp -> {
            try {
                if (resp.statusCode() != 200) return false;
                JsonNode raiz = JSON.readTree(resp.body());
                if (!"success".equals(raiz.path("result").asText())) return false;
                JsonNode rates = raiz.path("rates");
                Map<Moneda, BigDecimal> nuevas = referenciales();
                for (Moneda m : Moneda.values()) {
                    if (m == Moneda.GTQ) continue;
                    JsonNode t = rates.get(m.name());
                    if (t == null || !t.isNumber() || t.decimalValue().signum() <= 0) return false;   
                    nuevas.put(m, t.decimalValue());
                }
                tasas = nuevas;
                enLinea = true;
                ultimaConsulta = System.currentTimeMillis();
                String f = raiz.path("time_last_update_utc").asText("");
                actualizadoEl = f.length() >= 16 ? f.substring(0, 16) : f;       
                return true;
            } catch (Exception e) {
                return false;
            }
        }).exceptionally(e -> false);
    }
}
