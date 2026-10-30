package com.sistematickets.Controller;

import com.sistematickets.Model.Concierto;
import com.sistematickets.Model.InventarioBoletos;
import com.sistematickets.Service.ImagenService;
import com.sistematickets.Service.InventarioService;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.*;

public class BoletosClienteController {

    private static int maxBoletos() { return ClienteUI.esVendedor() ? 20 : 8; }

    @FXML private HBox encabezado;
    @FXML private ImageView imgEvento;
    @FXML private Label lblArtista;
    @FXML private Label lblEvento;
    @FXML private Label lblFecha;
    @FXML private Label lblRecinto;
    @FXML private StackPane zonaMapa;
    @FXML private VBox listaTipos;
    @FXML private VBox lineasResumen;
    @FXML private Label lblVacio;
    @FXML private Label lblBoletos;
    @FXML private Label lblSubtotal;
    @FXML private Label lblAviso;
    @FXML private Button btnContinuar;
    @FXML private Button btnVolver;

    private final InventarioService inventarioService = new InventarioService();
    private final Map<Integer, TipoCard> tipos = new LinkedHashMap<>();
    private MapaRecinto mapa;
    private Integer activo;

    @FXML
    private void initialize() {
        ClienteUI.encabezado(encabezado, 2);
        if (ClienteUI.esVendedor()) btnVolver.setText("← Volver al menú de conciertos");
        Concierto c = FlujoCompra.concierto();
        if (c == null) {                       // se llegó sin elegir concierto
            Platform.runLater(() -> ClienteUI.ir(ClienteUI.inicio()));
            return;
        }
        pintarCabecera(c);
        lblVacio.setText("Cargando localidades…");
        refrescarResumen();
        inventarioService.porConcierto(c.idConcierto()).whenComplete((lista, ex) -> Platform.runLater(() -> {
            if (ex != null) {
                lblVacio.setText("No se pudo cargar el inventario: " + ClienteUI.mensajeDe(ex));
                return;
            }
            construir(lista == null ? List.of() : lista);
        }));
    }

    private void pintarCabecera(Concierto c) {
        lblArtista.setText(c.nombreArtista() == null || c.nombreArtista().isBlank() ? c.tituloEvento() : c.nombreArtista());
        lblEvento.setText(c.tituloEvento() == null ? "" : c.tituloEvento());
        lblFecha.setText(ClienteUI.fechaLarga(ClienteUI.fecha(c)));
        lblRecinto.setText(c.recinto() == null ? "" : c.recinto());
        Rectangle clip = new Rectangle(imgEvento.getFitWidth(), imgEvento.getFitHeight());
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        imgEvento.setClip(clip);
        ImagenService.getInstance().mostrar(imgEvento, c.imagenPath(), ClienteUI.placeholder(),
                img -> ClienteUI.recortar(imgEvento, img));
    }

    private void construir(List<InventarioBoletos> lista) {
        List<InventarioBoletos> orden = new ArrayList<>(lista);
        orden.sort(Comparator.comparing(InventarioBoletos::precio, Comparator.nullsLast(Comparator.reverseOrder())));

        for (InventarioBoletos inv : orden) {
            int previa = FlujoCompra.cantidadDe(inv.idInventario());
            if (previa > inv.disponibles()) FlujoCompra.cantidad(inv, inv.disponibles());
        }

        mapa = new MapaRecinto(orden, this::elegir);
        zonaMapa.getChildren().setAll(mapa);

        tipos.clear();
        listaTipos.getChildren().clear();
        for (int i = 0; i < orden.size(); i++) {
            InventarioBoletos inv = orden.get(i);
            TipoCard card = new TipoCard(inv, MapaRecinto.colorDe(i * 5 / Math.max(1, orden.size() - 1)));
            tipos.put(inv.idInventario(), card);
            listaTipos.getChildren().add(card.raiz);
        }
        if (orden.isEmpty()) lblVacio.setText("Este concierto aún no tiene boletos a la venta.");
        activo = tipos.entrySet().stream().filter(e -> FlujoCompra.cantidadDe(e.getKey()) > 0)
                .map(Map.Entry::getKey).findFirst().orElse(null);
        refrescar();
    }


    private void elegir(Integer idInventario) {
        TipoCard t = tipos.get(idInventario);
        if (t == null || t.inv.disponibles() == 0) return;
        boolean cambio = !Objects.equals(activo, idInventario);
        activo = idInventario;
        refrescar();
        if (cambio) {
            FadeTransition ft = new FadeTransition(Duration.millis(200), t.cuerpo);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }

    private int maximoPara(InventarioBoletos inv) {
        int otros = FlujoCompra.totalBoletos() - FlujoCompra.cantidadDe(inv.idInventario());
        return Math.max(0, Math.min(inv.disponibles(), maxBoletos() - otros));
    }

    private void refrescar() {
        Set<Integer> conBoletos = new HashSet<>();
        for (TipoCard t : tipos.values()) {
            int cant = FlujoCompra.cantidadDe(t.inv.idInventario());
            if (cant > 0) conBoletos.add(t.inv.idInventario());
            t.actualizar(cant, Objects.equals(activo, t.inv.idInventario()));
        }
        refrescarResumen();                     
        try {
            if (mapa != null) mapa.actualizar(conBoletos, activo);
        } catch (RuntimeException ex) {
            System.err.println("[Boletos] No se pudo resaltar el mapa: " + ex);
        }
    }

    private void refrescarResumen() {
        lineasResumen.getChildren().clear();
        List<LineaCompra> lineas = FlujoCompra.lineas();
        boolean vacio = lineas.isEmpty();
        lblVacio.setVisible(vacio);
        lblVacio.setManaged(vacio);
        if (vacio && !tipos.isEmpty()) lblVacio.setText("Elige una zona del mapa para empezar.");

        for (LineaCompra l : lineas) {
            Label nombre = new Label(l.cantidad() + " × " + l.localidad());
            nombre.getStyleClass().add("cli-linea");
            Region sep = new Region();
            HBox.setHgrow(sep, Priority.ALWAYS);
            Label monto = new Label(Impuestos.q(l.subtotal()));
            monto.getStyleClass().add("cli-linea-monto");
            HBox fila = new HBox(8, nombre, sep, monto);
            fila.setAlignment(Pos.CENTER_LEFT);
            lineasResumen.getChildren().add(fila);
        }
        int n = FlujoCompra.totalBoletos();
        lblBoletos.setText(n == 0 ? "Sin boletos" : n + (n == 1 ? " boleto" : " boletos"));
        lblSubtotal.setText(Impuestos.q(FlujoCompra.total()));
        btnContinuar.setDisable(n == 0);
        boolean tope = n >= maxBoletos();
        lblAviso.setText(tope ? "Llegaste al máximo de " + maxBoletos() + " boletos por orden." : "Máximo " + maxBoletos() + " boletos por orden · IVA incluido");
    }

    @FXML
    private void onContinuar() {
        if (FlujoCompra.totalBoletos() == 0) return;
        ClienteUI.ir("cliente-pago");
    }

    @FXML
    private void onVolver() {
        ClienteUI.ir(ClienteUI.inicio());
    }

    private final class TipoCard {
        final InventarioBoletos inv;
        final VBox raiz = new VBox(10);
        final VBox cuerpo = new VBox(8);
        final Label lblCant = new Label("0");
        final Label insignia = new Label();
        final Button menos = new Button("−");
        final Button mas = new Button("+");

        TipoCard(InventarioBoletos inv, String color) {
            this.inv = inv;
            boolean agotada = inv.disponibles() == 0;

            Region punto = new Region();
            punto.setMinSize(14, 14);
            punto.setPrefSize(14, 14);
            punto.setMaxSize(14, 14);
            punto.setStyle("-fx-background-radius: 7; -fx-background-color: " + (agotada ? "#3b3550" : color) + ";");

            Label nombre = new Label(inv.localidad());
            nombre.getStyleClass().add("cli-zona-nombre");
            Label disp = new Label(agotada ? "Agotado" : inv.disponibles() + " disponibles");
            disp.getStyleClass().add("cli-zona-disp");
            VBox izq = new VBox(2, nombre, disp);

            Region sep = new Region();
            HBox.setHgrow(sep, Priority.ALWAYS);
            Label precio = new Label(Impuestos.q(inv.precio()));
            precio.getStyleClass().add("cli-zona-precio");
            insignia.getStyleClass().add("cli-chip");
            insignia.setVisible(false);
            insignia.setManaged(false);
            VBox der = new VBox(4, precio, insignia);
            der.setAlignment(Pos.CENTER_RIGHT);

            HBox cabecera = new HBox(12, punto, izq, sep, der);
            cabecera.setAlignment(Pos.CENTER_LEFT);

            Label cantidad = new Label("Cantidad");
            cantidad.getStyleClass().add("cli-zona-disp");
            menos.getStyleClass().add("cli-qty-btn");
            mas.getStyleClass().add("cli-qty-btn");
            lblCant.getStyleClass().add("cli-qty-val");
            lblCant.setMinWidth(40);
            lblCant.setAlignment(Pos.CENTER);
            Region sep2 = new Region();
            HBox.setHgrow(sep2, Priority.ALWAYS);
            HBox selector = new HBox(10, cantidad, sep2, menos, lblCant, mas);
            selector.setAlignment(Pos.CENTER_LEFT);
            cuerpo.getChildren().add(selector);
            cuerpo.setPadding(new Insets(4, 0, 0, 26));

            raiz.getChildren().addAll(cabecera, cuerpo);
            raiz.getStyleClass().add("cli-zona");
            if (agotada) raiz.getStyleClass().add("agotada");
            raiz.setOnMouseClicked(e -> elegir(inv.idInventario()));
            menos.setOnAction(e -> cambiar(-1));
            mas.setOnAction(e -> cambiar(1));
            mostrarCuerpo(false);
        }

        void cambiar(int delta) {
            int actual = FlujoCompra.cantidadDe(inv.idInventario());
            int nueva = Math.max(0, Math.min(actual + delta, maximoPara(inv)));
            FlujoCompra.cantidad(inv, nueva);
            activo = inv.idInventario();
            refrescar();
        }

        void actualizar(int cantidad, boolean esActivo) {
            lblCant.setText(String.valueOf(cantidad));
            insignia.setText("× " + cantidad);
            insignia.setVisible(cantidad > 0);
            insignia.setManaged(cantidad > 0);
            menos.setDisable(cantidad <= 0);
            mas.setDisable(cantidad >= maximoPara(inv));
            mostrarCuerpo(esActivo);
            raiz.getStyleClass().remove("activa");
            if (esActivo) raiz.getStyleClass().add("activa");
        }

        private void mostrarCuerpo(boolean ver) {
            cuerpo.setVisible(ver);
            cuerpo.setManaged(ver);
        }
    }
}
