package com.sistematickets.Controller;

import com.sistematickets.Model.Concierto;
import com.sistematickets.Model.InventarioBoletos;
import com.sistematickets.Service.ConciertoService;
import com.sistematickets.Service.InventarioService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ConciertoController {
    private final ConciertoService conciertoService = new ConciertoService();
    private final InventarioService inventarioService = new InventarioService();

    public CompletableFuture<List<Concierto>> cargarConciertos() {
        return conciertoService.listar();
    }

    public CompletableFuture<List<InventarioBoletos>> cargarInventario(int idConcierto) {
        return inventarioService.porConcierto(idConcierto);
    }

    /** Solo localidades con boletos disponibles. */
    public CompletableFuture<List<InventarioBoletos>> cargarDisponibles(int idConcierto) {
        return inventarioService.porConcierto(idConcierto).thenApply(l -> l.stream()
                .filter(i -> i.cantidadDisponible() != null && i.cantidadDisponible() > 0)
                .toList());
    }
}
