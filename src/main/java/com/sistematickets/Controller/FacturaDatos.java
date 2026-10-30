package com.sistematickets.Controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


public record FacturaDatos(
        String numero,
        String fechaTexto,
        String clienteNombre,
        String clienteDoc,
        String atendidoPor,
        String evento,
        String artista,
        String fechaEvento,
        String recinto,
        List<LineaCompra> lineas,
        BigDecimal total,
        String metodo,
        String detallePago,
        String codigo) {

    public BigDecimal base() { return Impuestos.base(total); }

    public BigDecimal iva() { return Impuestos.iva(total); }


    public List<BoletoDatos> boletos() {
        int total = lineas.stream().mapToInt(LineaCompra::cantidad).sum();
        List<BoletoDatos> r = new ArrayList<>();
        int n = 0;
        for (LineaCompra l : lineas) {
            for (int i = 0; i < l.cantidad(); i++) {
                n++;
                r.add(new BoletoDatos(codigo + "-" + String.format("%03d", n), l.localidad(), l.precio(), n, total));
            }
        }
        return r;
    }

   
    public String nombreArchivo() {
        return "Factura-" + numero.replaceAll("[^A-Za-z0-9-]", "") + ".pdf";
    }
}
