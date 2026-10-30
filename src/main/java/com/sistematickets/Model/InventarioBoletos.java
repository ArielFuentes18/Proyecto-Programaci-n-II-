package com.sistematickets.Model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;


@JsonIgnoreProperties(ignoreUnknown = true)
public record InventarioBoletos(
        @JsonProperty("id_inventario") @JsonAlias({"id", "idInventario"}) Integer idInventario,
        @JsonProperty("id_concierto") @JsonAlias({"idConcierto"}) Integer idConcierto,
        @JsonProperty("id_localidad") @JsonAlias({"idLocalidad"}) Integer idLocalidad,
        @JsonProperty("localidad") @JsonAlias({"nombre_localidad", "localidad_nombre", "nombreLocalidad"}) JsonNode localidadNode,
        BigDecimal precio,
        @JsonProperty("cantidad_total") @JsonAlias({"cantidadTotal"}) Integer cantidadTotal,
        @JsonProperty("cantidad_disponible") @JsonAlias({"cantidadDisponible"}) Integer cantidadDisponible) {

    public String localidad() {
        if (localidadNode != null) {
            if (localidadNode.isTextual()) return localidadNode.asText();
            if (localidadNode.isObject() && localidadNode.hasNonNull("nombre")) return localidadNode.get("nombre").asText();
        }
        return idLocalidad == null ? "Localidad" : "Localidad " + idLocalidad;
    }

    public int disponibles() {
        return cantidadDisponible == null ? 0 : Math.max(0, cantidadDisponible);
    }
}
