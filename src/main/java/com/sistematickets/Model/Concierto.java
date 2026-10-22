package com.sistematickets.Model;

import com.fasterxml.jackson.annotation.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Concierto(
        @JsonProperty("id_concierto") @JsonAlias({"id", "idConcierto"}) Integer idConcierto,
        @JsonProperty("id_artista") @JsonAlias({"idArtista"}) Integer idArtista,
        @JsonProperty("nombre_artistico") @JsonAlias({"nombreArtistico", "artista"}) String artista,
        @JsonProperty("titulo_evento") @JsonAlias({"tituloEvento", "titulo"}) String tituloEvento,
        @JsonProperty("fecha_concierto") @JsonAlias({"fechaConcierto", "fecha"}) String fechaConcierto,
        String recinto,
        String estado) {
}
