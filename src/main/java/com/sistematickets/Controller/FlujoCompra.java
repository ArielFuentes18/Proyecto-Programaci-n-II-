package com.sistematickets.Controller;

import com.sistematickets.Model.Concierto;
import com.sistematickets.Model.InventarioBoletos;
import com.sistematickets.Model.Venta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FlujoCompra {
    private FlujoCompra() {}

    private static Concierto concierto;
    private static final Map<Integer, LineaCompra> lineas = new LinkedHashMap<>();
    private static Venta venta;
    private static String metodoPago = "";
    private static String detallePago = "";
    private static String clienteNombre = "";
    private static String clienteNit = "";
    private static String clienteCorreo = "";
    private static boolean correoProcesado;

    public static void iniciar(Concierto c) {
        if (concierto == null || !Objects.equals(concierto.idConcierto(), c.idConcierto())) lineas.clear();
        concierto = c;
        venta = null;
        metodoPago = "";
        detallePago = "";
    }

    public static Concierto concierto() { return concierto; }

    public static void cantidad(InventarioBoletos inv, int cantidad) {
        if (cantidad <= 0) {
            lineas.remove(inv.idInventario());
        } else {
            lineas.put(inv.idInventario(), new LineaCompra(inv.idInventario(), inv.localidad(), inv.precio(), cantidad));
        }
    }

    public static int cantidadDe(Integer idInventario) {
        LineaCompra l = lineas.get(idInventario);
        return l == null ? 0 : l.cantidad();
    }

    public static List<LineaCompra> lineas() { return new ArrayList<>(lineas.values()); }

    public static int totalBoletos() { return lineas.values().stream().mapToInt(LineaCompra::cantidad).sum(); }


    public static BigDecimal total() {
        return lineas.values().stream().map(LineaCompra::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static void pagado(Venta v, String metodo, String detalle) {
        venta = v;
        metodoPago = metodo;
        detallePago = detalle;
        correoProcesado = false;
    }

    public static void datosCliente(String nombre, String nit) { datosCliente(nombre, nit, null); }


    public static void datosCliente(String nombre, String nit, String correo) {
        clienteCorreo = correo == null ? "" : correo.trim();
        clienteNombre = nombre == null || nombre.isBlank() ? "Consumidor final" : nombre.trim();
        clienteNit = nit == null || nit.isBlank() ? "CF" : nit.trim().toUpperCase();
    }

    public static String clienteNombre() { return clienteNombre.isEmpty() ? "Consumidor final" : clienteNombre; }
    public static String clienteNit() { return clienteNit.isEmpty() ? "CF" : clienteNit; }
    public static String clienteCorreo() { return clienteCorreo; }

    public static boolean marcarCorreoProcesado() {
        if (correoProcesado) return false;
        correoProcesado = true;
        return true;
    }

    public static Venta venta() { return venta; }
    public static String metodoPago() { return metodoPago; }
    public static String detallePago() { return detallePago; }

    public static void limpiarCarrito() {
        lineas.clear();
        venta = null;
        metodoPago = "";
        detallePago = "";
        clienteNombre = "";
        clienteNit = "";
        clienteCorreo = "";
        correoProcesado = false;
    }

    public static void reset() {
        limpiarCarrito();
        concierto = null;
    }
}
