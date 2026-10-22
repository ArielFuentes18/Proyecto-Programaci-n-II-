package com.sistematickets.Model;

import com.fasterxml.jackson.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Venta(
        @JsonProperty("id_venta") @JsonAlias({"id", "idVenta"}) Integer idVenta,
        @JsonProperty("id_usuario") @JsonAlias({"idUsuario"}) Integer idUsuario,
        @JsonProperty("fecha_venta") @JsonAlias({"fechaVenta"}) String fechaVenta,
        @JsonProperty("total_venta") @JsonAlias({"totalVenta"}) BigDecimal totalVenta,
        @JsonAlias({"detalle_ventas", "detalleVentas"}) List<DetalleVenta> detalles) {
}
