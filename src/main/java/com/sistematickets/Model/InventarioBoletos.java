package com.sistematickets.Model;

import com.fasterxml.jackson.annotation.*;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InventarioBoletos(
        @JsonProperty("id_inventario") @JsonAlias({"id", "idInventario"}) Integer idInventario,
        @JsonProperty("id_concierto") @JsonAlias({"idConcierto"}) Integer idConcierto,
        @JsonProperty("id_localidad") @JsonAlias({"idLocalidad"}) Integer idLocalidad,
        @JsonProperty("localidad") @JsonAlias({"nombre_localidad", "localidad_nombre", "nombreLocalidad"}) String localidad,
        BigDecimal precio,
        @JsonProperty("cantidad_total") @JsonAlias({"cantidadTotal"}) Integer cantidadTotal,
        @JsonProperty("cantidad_disponible") @JsonAlias({"cantidadDisponible"}) Integer cantidadDisponible) {
}
