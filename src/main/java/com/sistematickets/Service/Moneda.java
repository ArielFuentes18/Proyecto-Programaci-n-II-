package com.sistematickets.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public enum Moneda {
    GTQ("Q", "Quetzal guatemalteco", "1"),
    USD("US$", "Dólar estadounidense", "0.1307"),
    EUR("€", "Euro", "0.1125"),
    MXN("MX$", "Peso mexicano", "2.42"),
    CAD("CA$", "Dólar canadiense", "0.1810"),
    GBP("£", "Libra esterlina", "0.0980"),
    HNL("L", "Lempira hondureño", "3.44"),
    CRC("₡", "Colón costarricense", "65.5");

    private final String simbolo;
    private final String nombre;
    private final BigDecimal tasaReferencial;   

    Moneda(String simbolo, String nombre, String tasaReferencial) {
        this.simbolo = simbolo;
        this.nombre = nombre;
        this.tasaReferencial = new BigDecimal(tasaReferencial);
    }

    public String simbolo() { return simbolo; }
    public String nombre() { return nombre; }
    public BigDecimal tasaReferencial() { return tasaReferencial; }

    public String formatear(BigDecimal v) {
        if (v == null) v = BigDecimal.ZERO;
        return String.format(Locale.US, "%s%,.2f", simbolo, v.setScale(2, RoundingMode.HALF_UP));
    }


    public String etiqueta() { return name() + " · " + nombre; }
}
