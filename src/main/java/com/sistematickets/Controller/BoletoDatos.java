package com.sistematickets.Controller;

import java.math.BigDecimal;

/**
 * Un ticket individual de la compra.
 *
 * @param codigo   código único del ticket (es lo que lleva su QR), p. ej. TKT-000123-5-002
 * @param numero   posición del ticket dentro de la compra (1..total)
 * @param total    cantidad de tickets de la compra
 */
public record BoletoDatos(String codigo, String localidad, BigDecimal precio, int numero, int total) {
}
