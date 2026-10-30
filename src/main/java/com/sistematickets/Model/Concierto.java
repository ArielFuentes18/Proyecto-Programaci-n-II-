package com.sistematickets.Model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Concierto(
        @JsonProperty("id_concierto") @JsonAlias({"id", "idConcierto"}) Integer idConcierto,
        @JsonProperty("id_artista") @JsonAlias({"idArtista"}) Integer idArtista,
        @JsonProperty("titulo_evento") @JsonAlias({"tituloEvento", "titulo"}) String tituloEvento,
        @JsonProperty("fecha_concierto") @JsonAlias({"fechaConcierto", "fecha"}) String fechaConcierto,
        String recinto,
        String estado,
        @JsonProperty("nombre_artistico") @JsonAlias({"nombreArtistico"}) JsonNode nombreArtistico,
        @JsonProperty("imagen_url") @JsonAlias({"imagenUrl", "imagen_path", "imagenPath"}) String imagenUrl,
        @JsonProperty("artista") @JsonAlias({"artistas"}) JsonNode artistaNode) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Artista artista() {
        for (JsonNode n : new JsonNode[]{artistaNode, nombreArtistico}) {
            if (n != null && n.isObject()) {
                try {
                    return MAPPER.treeToValue(n, Artista.class);
                } catch (Exception ignorado) {
                }
            }
        }
        return null;
    }

    public String nombreArtista() {
        Artista a = artista();
        if (a != null && a.nombreArtistico() != null) return a.nombreArtistico();
        for (JsonNode n : new JsonNode[]{nombreArtistico, artistaNode}) {
            if (n != null && n.isTextual()) return n.asText();
        }
        return "";
    }

    public String imagenPath() {
        Artista a = artista();
        if (a != null && a.imagenUrl() != null && !a.imagenUrl().isBlank()) {
            return a.imagenUrl();
        }
        return imagenUrl;
    }

    public String genero() {
        Artista a = artista();
        return a != null && a.generoMusical() != null ? a.generoMusical().trim() : "";
    }

    public boolean vendible() {
        String e = estado == null ? "" : estado.trim().toLowerCase();
        return !(e.equals("cancelado") || e.equals("completado") || e.equals("finalizado") || e.equals("suspendido"));
    }
}
