package com.sistematickets.Model;

import com.fasterxml.jackson.annotation.*;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DetalleVenta(
        @JsonProperty("id_detalle") @JsonAlias({"idDetalle"}) Integer idDetalle,
        @JsonProperty("id_venta") @JsonAlias({"idVenta"}) Integer idVenta,
        @JsonProperty("id_inventario") @JsonAlias({"idInventario"}) Integer idInventario,
        Integer cantidad,
        @JsonProperty("precio_unitario") @JsonAlias({"precioUnitario"}) BigDecimal precioUnitario,
        BigDecimal subtotal) {

    public static DetalleVenta nuevo(Integer idInventario, int cantidad, BigDecimal precioUnitario) {
        return new DetalleVenta(null, null, idInventario, cantidad, precioUnitario,
                precioUnitario.multiply(BigDecimal.valueOf(cantidad)));
    }
}
