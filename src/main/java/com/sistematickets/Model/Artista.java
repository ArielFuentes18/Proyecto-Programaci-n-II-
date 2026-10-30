package com.sistematickets.Model;

import com.fasterxml.jackson.annotation.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Artista(
        @JsonProperty("id_artista") @JsonAlias({"id", "idArtista"}) Integer idArtista,
        @JsonProperty("nombre_artistico") @JsonAlias({"nombreArtistico", "nombre"}) String nombreArtistico,
        @JsonProperty("genero_musical") @JsonAlias({"generoMusical", "genero"}) String generoMusical,
        @JsonProperty("pais_origen") @JsonAlias({"paisOrigen", "pais"}) String paisOrigen,
        String descripcion,
        @JsonProperty("imagen_url") @JsonAlias({"imagenUrl", "imagen_path", "imagenPath"}) String imagenUrl) {

    public String imagenPath() {
        return imagenUrl;
    }
}