package com.sistematickets.Controller;

import java.math.BigDecimal;

public record LineaCompra(Integer idInventario, String localidad, BigDecimal precio, int cantidad) {
    public BigDecimal subtotal() {
        return precio.multiply(BigDecimal.valueOf(cantidad));
    }
}
