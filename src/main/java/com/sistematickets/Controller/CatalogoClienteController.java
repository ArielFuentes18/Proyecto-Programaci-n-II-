package com.sistematickets.Controller;

import com.sistematickets.Model.Concierto;
import com.sistematickets.Model.InventarioBoletos;
import com.sistematickets.Service.ConciertoService;
import com.sistematickets.Service.InventarioService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class CatalogoClienteController {

    private static final String TODOS_GENEROS = "Todos los géneros";
    private static final String TODOS_ARTISTAS = "Todos los artistas";
    private static final String CUALQUIER_FECHA = "Cualquier fecha";

    private record Resumen(BigDecimal desde, int disponibles) {}

    @FXML private HBox encabezado;
    @FXML private TextField tfBuscar;
    @FXML private ComboBox<String> cbGenero;
    @FXML private ComboBox<String> cbArtista;
    @FXML private ComboBox<String> cbFecha;
    @FXML private VBox seccionRecomendados;
    @FXML private FlowPane filaRecomendados;
    @FXML private FlowPane grid;
    @FXML private Label lblConteo;
    @FXML private Label lblEstado;

    private final ConciertoService conciertoService = new ConciertoService();
    private final InventarioService inventarioService = new InventarioService();
    private final List<Concierto> vendibles = new ArrayList<>();
    private final Map<Integer, Resumen> resumenes = new HashMap<>();

    @FXML
    private void initialize() {
        ClienteUI.encabezado(encabezado, 1);

        cbFecha.getItems().setAll(CUALQUIER_FECHA, "Esta semana", "Este mes", "Próximos 3 meses");
        cbFecha.setValue(CUALQUIER_FECHA);
        cbGenero.getItems().setAll(TODOS_GENEROS);
        cbGenero.setValue(TODOS_GENEROS);
        cbArtista.getItems().setAll(TODOS_ARTISTAS);
        cbArtista.setValue(TODOS_ARTISTAS);

        tfBuscar.textProperty().addListener((o, a, b) -> aplicarFiltros());
        cbGenero.valueProperty().addListener((o, a, b) -> aplicarFiltros());
        cbArtista.valueProperty().addListener((o, a, b) -> aplicarFiltros());
        cbFecha.valueProperty().addListener((o, a, b) -> aplicarFiltros());

        cargar();
    }

    private void cargar() {
        mostrarEstado("Cargando conciertos…");
        var fc = conciertoService.listar();
        var fi = inventarioService.todos().exceptionally(ex -> List.of());
        fc.thenCombine(fi, (cs, inv) -> new Object[]{cs, inv}).whenComplete((par, ex) -> Platform.runLater(() -> {
            if (ex != null) {
                mostrarEstado("No se pudieron cargar los conciertos: " + ClienteUI.mensajeDe(ex));
                return;
            }
            @SuppressWarnings("unchecked") List<Concierto> conciertos = (List<Concierto>) par[0];
            @SuppressWarnings("unchecked") List<InventarioBoletos> inventario = (List<InventarioBoletos>) par[1];
            recibir(conciertos == null ? List.of() : conciertos, inventario == null ? List.of() : inventario);
        }));
    }

    private void recibir(List<Concierto> conciertos, List<InventarioBoletos> inventario) {
        resumenes.clear();
        for (InventarioBoletos i : inventario) {
            if (i.idConcierto() == null) continue;
            Resumen previo = resumenes.get(i.idConcierto());
            int disp = i.disponibles();
            BigDecimal desde = previo == null ? null : previo.desde();
            if (disp > 0 && i.precio() != null && (desde == null || i.precio().compareTo(desde) < 0)) desde = i.precio();
            resumenes.put(i.idConcierto(), new Resumen(desde, (previo == null ? 0 : previo.disponibles()) + disp));
        }

        LocalDate hoy = LocalDate.now();
        vendibles.clear();
        for (Concierto c : conciertos) {
            if (!c.vendible()) continue;
            LocalDateTime f = ClienteUI.fecha(c);
            if (f != null && f.toLocalDate().isBefore(hoy)) continue;      // conciertos que ya pasaron
            vendibles.add(c);
        }
        vendibles.sort(Comparator.comparing(ClienteUI::fecha, Comparator.nullsLast(Comparator.naturalOrder())));

        llenarCombos();
        aplicarFiltros();
    }

    private void llenarCombos() {
        String g = cbGenero.getValue(), a = cbArtista.getValue();
        List<String> generos = vendibles.stream().map(Concierto::genero).filter(s -> !s.isBlank())
                .distinct().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
        List<String> artistas = vendibles.stream().map(Concierto::nombreArtista).filter(s -> s != null && !s.isBlank())
                .distinct().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
        generos.add(0, TODOS_GENEROS);
        artistas.add(0, TODOS_ARTISTAS);
        cbGenero.getItems().setAll(generos);
        cbArtista.getItems().setAll(artistas);
        cbGenero.setValue(generos.contains(g) ? g : TODOS_GENEROS);
        cbArtista.setValue(artistas.contains(a) ? a : TODOS_ARTISTAS);
    }

    private boolean hayFiltros() {
        return !tfBuscar.getText().isBlank()
                || !TODOS_GENEROS.equals(cbGenero.getValue())
                || !TODOS_ARTISTAS.equals(cbArtista.getValue())
                || !CUALQUIER_FECHA.equals(cbFecha.getValue());
    }

    private boolean cumple(Concierto c) {
        String q = tfBuscar.getText().trim().toLowerCase(ClienteUI.ES);
        if (!q.isEmpty()) {
            String texto = (c.nombreArtista() + " " + c.tituloEvento() + " " + c.recinto() + " " + c.genero()).toLowerCase(ClienteUI.ES);
            if (!texto.contains(q)) return false;
        }
        if (cbGenero.getValue() != null && !TODOS_GENEROS.equals(cbGenero.getValue())
                && !cbGenero.getValue().equalsIgnoreCase(c.genero())) return false;
        if (cbArtista.getValue() != null && !TODOS_ARTISTAS.equals(cbArtista.getValue())
                && !cbArtista.getValue().equals(c.nombreArtista())) return false;
        return cumpleFecha(c);
    }

    private boolean cumpleFecha(Concierto c) {
        String f = cbFecha.getValue();
        if (f == null || CUALQUIER_FECHA.equals(f)) return true;
        LocalDateTime d = ClienteUI.fecha(c);
        if (d == null) return false;
        LocalDate hoy = LocalDate.now();
        LocalDate dia = d.toLocalDate();
        return switch (f) {
            case "Esta semana" -> !dia.isAfter(hoy.plusDays(7));
            case "Este mes" -> YearMonth.from(dia).equals(YearMonth.from(hoy));
            case "Próximos 3 meses" -> !dia.isAfter(hoy.plusMonths(3));
            default -> true;
        };
    }

    @FXML
    private void onLimpiar() {
        tfBuscar.clear();
        cbGenero.setValue(TODOS_GENEROS);
        cbArtista.setValue(TODOS_ARTISTAS);
        cbFecha.setValue(CUALQUIER_FECHA);
    }

    private void aplicarFiltros() {
        if (vendibles.isEmpty()) {
            grid.getChildren().clear();
            filaRecomendados.getChildren().clear();
            ocultar(seccionRecomendados);
            lblConteo.setText("");
            mostrarEstado("Por ahora no hay conciertos disponibles. Vuelve pronto.");
            return;
        }
        List<Concierto> lista = vendibles.stream().filter(this::cumple).collect(Collectors.toList());

        pintarRecomendados();

        List<Node> tarjetas = new ArrayList<>();
        for (Concierto c : lista) tarjetas.add(tarjeta(c, false));
        grid.getChildren().setAll(tarjetas);
        lblConteo.setText(lista.size() + (lista.size() == 1 ? " concierto" : " conciertos"));
        if (lista.isEmpty()) mostrarEstado("No encontramos conciertos con esos filtros. Prueba con otra búsqueda.");
        else ocultar(lblEstado);
    }

    private void pintarRecomendados() {
        List<Concierto> top = new ArrayList<>();
        if (!hayFiltros()) {
            Set<String> artistas = new HashSet<>();
            for (Concierto c : vendibles) {
                Resumen r = resumenes.get(c.idConcierto());
                if (r == null || r.disponibles() <= 0) continue;
                if (!artistas.add(String.valueOf(c.nombreArtista()))) continue;
                top.add(c);
                if (top.size() == 3) break;
            }
        }
        if (top.isEmpty()) {
            filaRecomendados.getChildren().clear();
            ocultar(seccionRecomendados);
            return;
        }
        List<Node> nodos = new ArrayList<>();
        for (Concierto c : top) nodos.add(tarjeta(c, true));
        filaRecomendados.getChildren().setAll(nodos);
        seccionRecomendados.setVisible(true);
        seccionRecomendados.setManaged(true);
    }

    private Node tarjeta(Concierto c, boolean destacada) {
        Resumen r = resumenes.get(c.idConcierto());
        return TarjetaCliente.crear(c, r == null ? null : r.desde(), r == null ? 0 : r.disponibles(), destacada, this::elegir);
    }

    private void elegir(Concierto c) {
        FlujoCompra.iniciar(c);
        ClienteUI.ir("cliente-boletos");
    }

    private void mostrarEstado(String texto) {
        lblEstado.setText(texto);
        lblEstado.setVisible(true);
        lblEstado.setManaged(true);
    }

    private static void ocultar(Node n) {
        n.setVisible(false);
        n.setManaged(false);
    }
}
