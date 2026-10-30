package com.sistematickets.Controller;

import com.sistematickets.Service.ApiException;
import com.sistematickets.Service.EstadisticaService;
import com.sistematickets.Service.ReporteService;
import com.sistematickets.Service.ReporteService.Dato;
import com.sistematickets.Service.ReporteService.Tipo;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import net.sf.jasperreports.engine.JasperPrint;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EstadisticasController {

    private record Periodo(String texto, int dias) {
        @Override public String toString() { return texto; }
    }

    private record Espec(String titulo, String subtitulo, Tipo tipo, List<Dato> datos, String nota) {}

    private static final double ANCHO_GRAFICO = 500;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private ComboBox<Periodo> cbPeriodo;
    @FXML private Button btnActualizar;
    @FXML private Button btnPdf;
    @FXML private Label lblEstado;
    @FXML private GridPane kpis;
    @FXML private FlowPane graficos;
    @FXML private VBox hallazgos;
    @FXML private TableView<Map<String, Object>> tabla;

    private final EstadisticaService servicio = new EstadisticaService();
    private final ExecutorService hilo = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "estadisticas");
        t.setDaemon(true);
        return t;
    });
    private Map<String, Object> datos;
    private JasperPrint[] prints = new JasperPrint[0];
    private int generacion;

    @FXML
    private void initialize() {
        cbPeriodo.getItems().addAll(new Periodo("Últimos 7 días", 7), new Periodo("Últimos 30 días", 30),
                new Periodo("Últimos 90 días", 90), new Periodo("Último año", 365), new Periodo("Todo el historial", 0));
        cbPeriodo.getSelectionModel().select(1);
        cbPeriodo.setOnAction(e -> cargar());
        btnPdf.setDisable(true);
        construirTabla();
        kpis.widthProperty().addListener((o, a, b) -> ordenarKpis());   
    }

    @FXML private void onActualizar() { cargar(); }

    public void cargar() {
        int gen = ++generacion;
        Periodo p = cbPeriodo.getValue();
        estado("Cargando estadísticas…", false);
        btnActualizar.setDisable(true);
        btnPdf.setDisable(true);
        servicio.resumen(p == null ? 30 : p.dias()).whenComplete((r, ex) -> Platform.runLater(() -> {
            if (gen != generacion) return;
            btnActualizar.setDisable(false);
            if (ex != null) {
                estado(mensaje(ex), true);
                return;
            }
            datos = r;
            pintar();
            estado("Actualizado a las " + LocalDateTime.now().format(HORA) + ".", false);
            btnPdf.setDisable(false);
            renderizarGraficos(gen);
        }));
    }

    private void pintar() {
        Map<String, Object> k = mapa("kpis");
        int dias = (int) n(mapa("periodo").get("dias"));
        tarjetasKpi = java.util.List.of(
                kpi("Ingresos", q(n(k.get("ingresos"))), delta(k.get("variacion_ingresos"), dias)),
                kpi("Boletos vendidos", entero(n(k.get("boletos"))), delta(k.get("variacion_boletos"), dias)),
                kpi("Ventas realizadas", entero(n(k.get("ventas"))), delta(k.get("variacion_ventas"), dias)),
                kpi("Ticket promedio", q(n(k.get("ticket_promedio"))), String.format(Locale.US, "%.1f boletos por venta", n(k.get("boletos_por_venta")))),
                kpi("Ocupación global", pct(n(k.get("ocupacion_pct"))), entero(n(k.get("boletos_vendidos_total"))) + " de " + entero(n(k.get("capacidad_total"))) + " boletos"),
                kpi("Por vender", q(n(k.get("ingresos_por_realizar"))), "de " + q(n(k.get("ingresos_potenciales"))) + " potenciales"),
                kpi("Conciertos en riesgo", entero(n(k.get("conciertos_riesgo"))), "de " + entero(n(k.get("conciertos_proximos"))) + " próximos"));
        columnasKpi = -1;        
        ordenarKpis();

        hallazgos.getChildren().clear();
        for (Object h : lista("hallazgos", Object.class)) {
            Label l = new Label("• " + h);
            l.getStyleClass().add("hallazgo");
            l.setWrapText(true);
            hallazgos.getChildren().add(l);
        }
        tabla.getItems().setAll(lista("por_concierto"));
    }

    private java.util.List<VBox> tarjetasKpi = java.util.List.of();
    private int columnasKpi = -1;

 
    private void ordenarKpis() {
        double ancho = kpis.getWidth();
        int columnas = ancho >= 1450 ? 7 : ancho >= 700 ? 4 : 2;
        if (columnas == columnasKpi && kpis.getChildren().size() == tarjetasKpi.size()) return;
        columnasKpi = columnas;
        kpis.getChildren().clear();
        kpis.getColumnConstraints().clear();
        for (int i = 0; i < columnas; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / columnas);
            cc.setHgrow(Priority.ALWAYS);
            kpis.getColumnConstraints().add(cc);
        }
        for (int i = 0; i < tarjetasKpi.size(); i++) {
            VBox c = tarjetasKpi.get(i);
            c.setMaxWidth(Double.MAX_VALUE);
            GridPane.setFillWidth(c, true);
            GridPane.setFillHeight(c, true);
            kpis.add(c, i % columnas, i / columnas);
        }
    }

    private VBox kpi(String titulo, String valor, String sub) {
        Label t = new Label(titulo);
        t.getStyleClass().add("kpi-title");
        Label v = new Label(valor);
        v.getStyleClass().add("kpi-value");
        Label s = new Label(sub);
        s.setWrapText(true);
        s.getStyleClass().add(sub.startsWith("▼") ? "kpi-down" : sub.startsWith("▲") ? "kpi-up" : "kpi-sub");
        VBox c = new VBox(4, t, v, s);
        c.getStyleClass().add("kpi-card");
        return c;
    }

    private String delta(Object variacion, int dias) {
        if (dias == 0) return "Todo el historial";
        if (!(variacion instanceof Number x)) return "Sin ventas en el período anterior";
        double d = x.doubleValue();
        return (d >= 0 ? "▲ " : "▼ ") + String.format(Locale.US, "%.1f%%", Math.abs(d)) + " vs período anterior";
    }

    private void construirTabla() {
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("Sin conciertos para mostrar."));
        tabla.getColumns().add(col("Concierto", c -> s(c.get("titulo")), false));
        tabla.getColumns().add(col("Cantante", c -> s(c.get("artista")), false));
        tabla.getColumns().add(col("Fecha", c -> fechaCorta(s(c.get("fecha"))), false));
        tabla.getColumns().add(col("Ocupación", c -> pct(n(c.get("ocupacion_pct"))), false));
        tabla.getColumns().add(col("Boletos", c -> entero(n(c.get("vendidos"))) + " / " + entero(n(c.get("capacidad"))), false));
        tabla.getColumns().add(col("Ingresos período", c -> q(n(c.get("ingresos_periodo"))), false));
        tabla.getColumns().add(col("Puntaje", c -> entero(n(c.get("puntaje"))), false));
        tabla.getColumns().add(col("Demanda", c -> s(c.get("demanda")) + (Boolean.TRUE.equals(c.get("riesgo")) ? " · riesgo" : ""), true));
        TableColumn<Map<String, Object>, String> rec = col("Recomendación", c -> s(c.get("recomendacion")), false);
        rec.setMinWidth(220);
        tabla.getColumns().add(rec);
    }

    private TableColumn<Map<String, Object>, String> col(String titulo, Function<Map<String, Object>, String> f, boolean pill) {
        TableColumn<Map<String, Object>, String> c = new TableColumn<>(titulo);
        c.setSortable(false);
        c.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(f.apply(d.getValue())));
        c.setCellFactory(tc -> new TableCell<Map<String, Object>, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                if (pill) {
                    setText(null);
                    Label p = new Label(item);
                    p.getStyleClass().add("pill");
                    setGraphic(p);
                } else {
                    setGraphic(null);
                    setText(item);
                    setWrapText(true);
                }
            }
        });
        return c;
    }

    private List<Espec> especificaciones() {
        int dias = (int) n(mapa("periodo").get("dias"));
        String rango = dias == 0 ? "Todo el historial" : "Últimos " + dias + " días";
        String sub = rango + " · Quetzales (Q)";
        double totalIngresos = n(mapa("kpis").get("ingresos"));
        List<Espec> out = new ArrayList<>();

        boolean diaria = dias > 0 && dias <= 31;
        List<Dato> tendencia = new ArrayList<>();
        double mejor = -1;
        String mejorEt = "";
        for (Map<String, Object> p : lista(diaria ? "serie_diaria" : "serie_mensual")) {
            String et = diaria ? etiquetaDia(s(p.get("fecha"))) : etiquetaMes(s(p.get("mes")));
            double v = n(p.get("ingresos"));
            tendencia.add(new Dato(et, v));
            if (v > mejor) { mejor = v; mejorEt = et; }
        }
        boolean hayVentas = tendencia.stream().anyMatch(d -> d.valor() > 0);
        out.add(new Espec(diaria ? "Evolución de ingresos diarios" : "Evolución de ingresos mensuales",
                diaria ? sub : "Últimos 12 meses · Quetzales (Q)", Tipo.LINEAS, hayVentas ? tendencia : List.of(),
                "Mejor " + (diaria ? "día" : "mes") + ": " + mejorEt + " con " + q(mejor) + "."));


        List<Map<String, Object>> cons = lista("por_concierto");
        List<Dato> porConcierto = cons.stream()
                .filter(c -> n(c.get("ingresos_periodo")) > 0)
                .sorted(Comparator.comparingDouble((Map<String, Object> c) -> n(c.get("ingresos_periodo"))).reversed())
                .limit(8)
                .map(c -> new Dato(corta(s(c.get("titulo")), 22), n(c.get("ingresos_periodo"))))
                .collect(Collectors.toList());
        out.add(new Espec("Ingresos por concierto", sub, Tipo.BARRAS, porConcierto,
                porConcierto.isEmpty() ? "" : "Líder: " + porConcierto.get(0).etiqueta() + " con " + q(porConcierto.get(0).valor())
                        + " (" + pct(totalIngresos > 0 ? porConcierto.get(0).valor() / totalIngresos * 100 : 0) + " de los ingresos)."));

   
        List<Dato> ocupacion = cons.stream()
                .filter(c -> n(c.get("capacidad")) > 0 && !finalizado(c))
                .sorted(Comparator.comparingDouble((Map<String, Object> c) -> n(c.get("ocupacion_pct"))).reversed())
                .limit(8)
                .map(c -> new Dato(corta(s(c.get("titulo")), 22), n(c.get("ocupacion_pct"))))
                .collect(Collectors.toList());
        long bajas = ocupacion.stream().filter(d -> d.valor() < 40).count();
        out.add(new Espec("Ocupación de boletos por concierto", "Porcentaje de boletos vendidos (conciertos próximos)", Tipo.BARRAS, ocupacion,
                bajas + " de " + ocupacion.size() + " conciertos próximos tienen ocupación menor al 40%. En riesgo: "
                        + entero(n(mapa("kpis").get("conciertos_riesgo"))) + "."));

        List<Dato> porLoc = lista("por_localidad").stream().filter(l -> n(l.get("ingresos")) > 0)
                .map(l -> new Dato(s(l.get("nombre")), n(l.get("ingresos")))).collect(Collectors.toList());
        out.add(new Espec("Ingresos por localidad", sub, Tipo.PASTEL, porLoc,
                porLoc.isEmpty() ? "" : "Localidad líder: " + porLoc.get(0).etiqueta() + " ("
                        + pct(totalIngresos > 0 ? porLoc.get(0).valor() / totalIngresos * 100 : 0) + " de los ingresos)."));

        List<Dato> porGen = lista("por_genero").stream().filter(g -> n(g.get("ingresos")) > 0)
                .map(g -> new Dato(s(g.get("genero")), n(g.get("ingresos")))).collect(Collectors.toList());
        out.add(new Espec("Ingresos por género musical", sub, Tipo.PASTEL, porGen,
                porGen.isEmpty() ? "" : "Género con más ingresos: " + porGen.get(0).etiqueta() + " (" + q(porGen.get(0).valor()) + ")."));

        
        List<Map<String, Object>> vend = lista("top_vendedores");
        List<Dato> porVend = vend.stream().filter(v -> n(v.get("ingresos")) > 0).limit(8)
                .map(v -> new Dato(corta(s(v.get("nombre")), 18), n(v.get("ingresos")))).collect(Collectors.toList());
        out.add(new Espec("Ingresos por vendedor", sub, Tipo.BARRAS, porVend,
                porVend.isEmpty() ? "" : "Mejor vendedor: " + s(vend.get(0).get("nombre")) + " con " + q(n(vend.get(0).get("ingresos")))
                        + " en " + entero(n(vend.get(0).get("ventas"))) + " ventas."));
        return out;
    }

    private void renderizarGraficos(int gen) {
        graficos.getChildren().clear();
        List<Espec> specs = especificaciones();
        JasperPrint[] arr = new JasperPrint[specs.size()];
        prints = arr;
        List<StackPane> tarjetas = new ArrayList<>();
        for (int i = 0; i < specs.size(); i++) {
            Label l = new Label("Generando gráfico…");
            l.getStyleClass().add("chart-msg");
            StackPane sp = new StackPane(l);
            sp.getStyleClass().add("chart-card");
            sp.setPrefSize(ANCHO_GRAFICO, ANCHO_GRAFICO * 595 / 842.0);
            sp.setMinSize(ANCHO_GRAFICO, ANCHO_GRAFICO * 595 / 842.0);
            sp.setMaxSize(ANCHO_GRAFICO, ANCHO_GRAFICO * 595 / 842.0);
            tarjetas.add(sp);
            graficos.getChildren().add(sp);
        }
        String pie = "Sistema de Venta de Tickets · Generado el " + LocalDate.now() + " " + LocalDateTime.now().format(HORA);
        hilo.submit(() -> {
            for (int i = 0; i < specs.size(); i++) {
                if (gen != generacion) return;
                final int idx = i;
                final Espec e = specs.get(i);
                final StackPane tarjeta = tarjetas.get(i);
                if (e.datos().isEmpty()) {
                    Platform.runLater(() -> mensajeEn(tarjeta, e.titulo() + ": sin datos en este período."));
                    continue;
                }
                try {
                    JasperPrint p = ReporteService.grafico(e.tipo(), e.titulo(), e.subtitulo(), e.nota(), pie, e.datos());
                    byte[] png = ReporteService.aPng(p, 1.4f);
                    arr[idx] = p;
                    Platform.runLater(() -> {
                        ImageView iv = new ImageView(new Image(new ByteArrayInputStream(png)));
                        iv.setFitWidth(ANCHO_GRAFICO - 8);
                        iv.setPreserveRatio(true);
                        iv.setSmooth(true);
                        tarjeta.getChildren().setAll(iv);
                    });
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                    Platform.runLater(() -> mensajeEn(tarjeta, "No se pudo generar «" + e.titulo() + "»: " + msg));
                }
            }
        });
    }

    private void mensajeEn(StackPane tarjeta, String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("chart-msg");
        l.setWrapText(true);
        tarjeta.getChildren().setAll(l);
    }

    // el pdf
    @FXML
    private void onExportarPdf() {
        if (datos == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar reporte gerencial");
        fc.setInitialFileName("reporte-gerencial-" + LocalDate.now() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File destino = fc.showSaveDialog(btnPdf.getScene().getWindow());
        if (destino == null) return;

        estado("Generando PDF…", false);
        btnPdf.setDisable(true);
        final Map<String, Object> snapshot = datos;
        final JasperPrint[] graf = prints;
        hilo.submit(() -> {
            try {
                List<JasperPrint> paginas = new ArrayList<>();
                paginas.add(resumenPrint(snapshot));
                for (JasperPrint p : graf) if (p != null) paginas.add(p);
                ReporteService.exportarPdf(paginas, destino);
                Platform.runLater(() -> { btnPdf.setDisable(false); estado("PDF guardado en: " + destino.getAbsolutePath(), false); });
            } catch (Throwable ex) {
                ex.printStackTrace();
                Platform.runLater(() -> { btnPdf.setDisable(false); estado("No se pudo generar el PDF: " + ex.getMessage(), true); });
            }
        });
    }

    @SuppressWarnings("unchecked")
    private JasperPrint resumenPrint(Map<String, Object> d) throws Exception {
        Map<String, Object> k = (Map<String, Object>) d.get("kpis");
        Map<String, Object> per = (Map<String, Object>) d.get("periodo");
        int dias = (int) n(per.get("dias"));
        String generado = LocalDate.now() + " " + LocalDateTime.now().format(HORA);
        Map<String, Object> params = new HashMap<>();
        params.put("TITULO", "Reporte gerencial de ventas");
        params.put("SUBTITULO", (dias == 0 ? "Todo el historial" : "Últimos " + dias + " días") + " · Generado el " + generado);
        params.put("PIE", "Sistema de Venta de Tickets · Reporte confidencial de uso interno");
        params.put("K_INGRESOS", q(n(k.get("ingresos"))));
        params.put("K_BOLETOS", entero(n(k.get("boletos"))));
        params.put("K_VENTAS", entero(n(k.get("ventas"))));
        params.put("K_TICKET", q(n(k.get("ticket_promedio"))));
        params.put("K_OCUPACION", pct(n(k.get("ocupacion_pct"))));
        params.put("K_POR_VENDER", q(n(k.get("ingresos_por_realizar"))));
        List<String> hs = new ArrayList<>();
        Object hl = d.get("hallazgos");
        if (hl instanceof List<?> l) for (Object o : l) hs.add("• " + o);
        params.put("HALLAZGOS", String.join("\n", hs));

        List<Map<String, String>> filas = new ArrayList<>();
        Object pc = d.get("por_concierto");
        if (pc instanceof List<?> l) {
            for (Object o : l) {
                Map<String, Object> c = (Map<String, Object>) o;
                Map<String, String> f = new LinkedHashMap<>();
                f.put("concierto", s(c.get("titulo")));
                f.put("cantante", s(c.get("artista")));
                f.put("fecha", fechaCorta(s(c.get("fecha"))));
                f.put("ocupacion", pct(n(c.get("ocupacion_pct"))));
                f.put("ingresos", q(n(c.get("ingresos_periodo"))));
                f.put("puntaje", entero(n(c.get("puntaje"))));
                f.put("demanda", s(c.get("demanda")) + (Boolean.TRUE.equals(c.get("riesgo")) ? " (riesgo)" : ""));
                f.put("recomendacion", s(c.get("recomendacion")));
                filas.add(f);
            }
        }
        return ReporteService.resumen(params, filas);
    }


    private void estado(String texto, boolean error) {
        lblEstado.setText(texto);
        lblEstado.getStyleClass().removeAll("muted", "err-label");
        lblEstado.getStyleClass().add(error ? "err-label" : "muted");
    }

    private String mensaje(Throwable t) {
        while (t instanceof CompletionException && t.getCause() != null) t = t.getCause();
        if (t instanceof ApiException ae && ae.getStatus() == 404) {
            return "El servidor no tiene la ruta de estadísticas. Despliega la versión nueva del backend.";
        }
        return t.getMessage() != null ? t.getMessage() : "No se pudieron cargar las estadísticas.";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapa(String clave) {
        Object o = datos == null ? null : datos.get(clave);
        return o instanceof Map<?, ?> ? (Map<String, Object>) o : new HashMap<>();
    }

    private List<Map<String, Object>> lista(String clave) {
        return lista(clave, null);
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> lista(String clave, Class<T> tipo) {
        Object o = datos == null ? null : datos.get(clave);
        return o instanceof List<?> ? (List<T>) o : new ArrayList<>();
    }

    private static boolean finalizado(Map<String, Object> c) {
        return c.get("dias_para_evento") instanceof Number x && x.doubleValue() < 0;
    }

    private static double n(Object o) { return o instanceof Number x ? x.doubleValue() : 0; }
    private static String s(Object o) { return o == null ? "" : String.valueOf(o); }
    private static String q(double v) { return String.format(Locale.US, "Q%,.2f", v); }
    private static String pct(double v) { return String.format(Locale.US, "%.1f%%", v); }
    private static String entero(double v) { return String.format(Locale.US, "%,d", Math.round(v)); }
    private static String corta(String t, int max) { return t.length() <= max ? t : t.substring(0, max - 1) + "…"; }

    private static String etiquetaDia(String iso) {
        return iso.length() >= 10 ? iso.substring(8, 10) + "/" + iso.substring(5, 7) : iso;
    }

    private static String etiquetaMes(String ym) {
        return ym.length() >= 7 ? ym.substring(5, 7) + "/" + ym.substring(0, 4) : ym;
    }

    private static String fechaCorta(String iso) {
        if (iso.isEmpty()) return "";
        try {
            return OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return iso.length() >= 10 ? iso.substring(0, 10) : iso;
        }
    }
}
