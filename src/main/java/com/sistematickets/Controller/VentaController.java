package com.sistematickets.Controller;

import com.sistematickets.Model.DetalleVenta;
import com.sistematickets.Model.InventarioBoletos;
import com.sistematickets.Model.Venta;
import com.sistematickets.Service.InventarioService;
import com.sistematickets.Service.VentaService;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Carrito en memoria + confirmación transaccional con revalidación de stock en tiempo real. */
public class VentaController {

    private record Item(InventarioBoletos inventario, int cantidad) {}

    private final VentaService ventaService = new VentaService();
    private final InventarioService inventarioService = new InventarioService();
    private final Map<Integer, Item> carrito = new LinkedHashMap<>();

    public void agregar(InventarioBoletos inv, int cantidad) {
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        int actual = carrito.containsKey(inv.idInventario()) ? carrito.get(inv.idInventario()).cantidad() : 0;
        int nueva = actual + cantidad;
        if (inv.cantidadDisponible() == null || nueva > inv.cantidadDisponible()) {
            throw new IllegalStateException("Boletos insuficientes en " + inv.localidad());
        }
        carrito.put(inv.idInventario(), new Item(inv, nueva));
    }

    public void quitar(int idInventario) { carrito.remove(idInventario); }

    public void vaciar() { carrito.clear(); }

    public boolean estaVacio() { return carrito.isEmpty(); }

    public int totalBoletos() { return carrito.values().stream().mapToInt(Item::cantidad).sum(); }

    public BigDecimal total() {
        return carrito.values().stream()
                .map(i -> i.inventario().precio().multiply(BigDecimal.valueOf(i.cantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<DetalleVenta> detallesActuales() {
        return carrito.values().stream()
                .map(i -> DetalleVenta.nuevo(i.inventario().idInventario(), i.cantidad(), i.inventario().precio()))
                .toList();
    }

    public CompletableFuture<Venta> confirmar() {
        if (carrito.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("El carrito está vacío"));
        }
        if (!Sesion.activa()) {
            return CompletableFuture.failedFuture(new IllegalStateException("No hay sesión activa"));
        }
        List<Item> items = List.copyOf(carrito.values());
        Integer idUsuario = Sesion.usuario().idUsuario();

        List<CompletableFuture<List<InventarioBoletos>>> consultas = items.stream()
                .map(i -> i.inventario().idConcierto()).distinct()
                .map(inventarioService::porConcierto).toList();

        return CompletableFuture.allOf(consultas.toArray(CompletableFuture[]::new)).thenCompose(v -> {
            Map<Integer, InventarioBoletos> actual = consultas.stream()
                    .flatMap(f -> f.join().stream())
                    .collect(Collectors.toMap(InventarioBoletos::idInventario, Function.identity(), (a, b) -> a));

            List<DetalleVenta> detalles = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (Item it : items) {
                InventarioBoletos inv = actual.get(it.inventario().idInventario());
                if (inv == null || inv.cantidadDisponible() < it.cantidad()) {
                    throw new IllegalStateException("Stock insuficiente en " + it.inventario().localidad()
                            + (inv == null ? "" : " (disponibles: " + inv.cantidadDisponible() + ")"));
                }
                DetalleVenta d = DetalleVenta.nuevo(inv.idInventario(), it.cantidad(), inv.precio());
                detalles.add(d);
                total = total.add(d.subtotal());
            }
            Venta req = new Venta(null, idUsuario, null, total, detalles);
            return ventaService.registrar(req);
        }).thenApply(venta -> {
            carrito.clear();
            return venta;
        });
    }
}
