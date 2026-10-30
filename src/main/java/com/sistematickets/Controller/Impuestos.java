package com.sistematickets.Controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Los precios del inventario YA incluyen IVA (12 % en Guatemala).
 * La factura separa la base imponible y el impuesto; el total pagado nunca cambia.
 */
public final class Impuestos {
    private Impuestos() {}

    public static final BigDecimal IVA = new BigDecimal("0.12");

    /** Valor sin IVA. */
    public static BigDecimal base(BigDecimal totalConIva) {
        return totalConIva.divide(BigDecimal.ONE.add(IVA), 2, RoundingMode.HALF_UP);
    }

    /** IVA contenido en el total (total - base, así base + iva == total siempre). */
    public static BigDecimal iva(BigDecimal totalConIva) {
        return totalConIva.setScale(2, RoundingMode.HALF_UP).subtract(base(totalConIva));
    }

    public static String q(BigDecimal v) {
        if (v == null) return "Q0.00";
        return String.format(Locale.US, "Q%,.2f", v.setScale(2, RoundingMode.HALF_UP));
    }
}
