package com.sistematickets.Controller;

import com.sistematickets.Model.Concierto;
import com.sistematickets.Model.DetalleVenta;
import com.sistematickets.Model.Venta;
import com.sistematickets.Service.ApiException;
import com.sistematickets.Service.Moneda;
import com.sistematickets.Service.TasasCambio;
import com.sistematickets.Service.VentaService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;


 
public class PagoClienteController {

    @FXML private HBox encabezado;
    @FXML private Label lblArtista;
    @FXML private Label lblEvento;
    @FXML private VBox lineasResumen;
    @FXML private Label lblBase;
    @FXML private Label lblIva;
    @FXML private Label lblTotal;

    @FXML private VBox panelCliente;
    @FXML private VBox boxCorreo;
    @FXML private Label lblTituloCliente;
    @FXML private TextField tfClienteCorreo;
    @FXML private Label errCorreo;
    @FXML private TextField tfClienteNombre;
    @FXML private TextField tfClienteNit;

    @FXML private ToggleButton tgEfectivo;
    @FXML private ToggleButton tgTarjeta;
    @FXML private ToggleButton tgTransferencia;
    @FXML private VBox panelEfectivo;
    @FXML private VBox panelTarjeta;
    @FXML private VBox panelTransferencia;

    @FXML private ComboBox<Moneda> cbMoneda;
    @FXML private Label lblTasa;
    @FXML private Label lblRecibidoMoneda;
    @FXML private HBox filaMoneda;
    @FXML private Label lblNombreMoneda;
    @FXML private Label lblTotalMoneda;

    @FXML private Label lblTotalCobrar;
    @FXML private TextField tfRecibido;
    @FXML private Label lblCambio;
    @FXML private Label errRecibido;

  
    @FXML private TextField tfTitular;
    @FXML private TextField tfNumero;
    @FXML private TextField tfVence;
    @FXML private PasswordField pfCvv;
    @FXML private Label errTitular;
    @FXML private Label errNumero;
    @FXML private Label errVence;
    @FXML private Label errCvv;

    @FXML private Label lblReferencia;
    @FXML private ComboBox<String> cbBanco;
    @FXML private TextField tfBoleta;
    @FXML private Label errBanco;
    @FXML private Label errBoleta;

    @FXML private Button btnPagar;
    @FXML private Label lblMensaje;
    @FXML private StackPane overlay;

    private final VentaService ventaService = new VentaService();
    private final String referencia = "REF-" + (100000 + ThreadLocalRandom.current().nextInt(900000));
    private boolean vendedor;
    private Moneda moneda = Moneda.GTQ;      

    @FXML
    private void initialize() {
        ClienteUI.encabezado(encabezado, 3);
        Concierto c = FlujoCompra.concierto();
        if (c == null || FlujoCompra.totalBoletos() == 0) {
            Platform.runLater(() -> ClienteUI.ir(ClienteUI.inicio()));
            return;
        }
        vendedor = ClienteUI.esVendedor();
        lblArtista.setText(c.nombreArtista() == null || c.nombreArtista().isBlank() ? c.tituloEvento() : c.nombreArtista());
        lblEvento.setText((c.tituloEvento() == null ? "" : c.tituloEvento()) + " · " + ClienteUI.fechaLarga(ClienteUI.fecha(c)));
        pintarResumen();

   
        mostrar(panelCliente, true);                       
        mostrar(boxCorreo, vendedor);                      
        if (!vendedor) {
            lblTituloCliente.setText("Datos de facturación (opcional)");
            var u = Sesion.usuario();
            if (u != null && u.nombre() != null) tfClienteNombre.setText(u.nombre());
        }
        mostrar(tgEfectivo, vendedor);
        mostrar(tgTarjeta, true);
        mostrar(tgTransferencia, !vendedor);
        
        ToggleGroup grupo = new ToggleGroup();
        tgEfectivo.setToggleGroup(grupo);
        tgTarjeta.setToggleGroup(grupo);
        tgTransferencia.setToggleGroup(grupo);
        (vendedor ? tgEfectivo : tgTarjeta).setSelected(true);      
        grupo.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null && a != null) a.setSelected(true);
            else cambiarMetodo();
        });
        cambiarMetodo();

        cbBanco.getItems().setAll("Banco Industrial", "Banrural", "BAC Credomatic", "Banco G&T Continental", "Otro banco");
        lblReferencia.setText(referencia);
        enlazarCampos();

        cbMoneda.getItems().setAll(Moneda.values());
        cbMoneda.setConverter(new StringConverter<>() {
            @Override public String toString(Moneda m) { return m == null ? "" : m.etiqueta(); }
            @Override public Moneda fromString(String s) { return null; }
        });
        cbMoneda.getSelectionModel().select(Moneda.GTQ);
        cbMoneda.valueProperty().addListener((o, a, b) -> {
            if (b == null) { cbMoneda.setValue(a); return; }
            moneda = b;
            refrescarMoneda();
        });
        refrescarMoneda();
        TasasCambio.actualizar().thenAccept(ok -> Platform.runLater(this::refrescarMoneda));
    }


    private BigDecimal totalPago() {
        return moneda == Moneda.GTQ ? FlujoCompra.total().setScale(2) : TasasCambio.convertir(FlujoCompra.total(), moneda);
    }

    private String fmt(BigDecimal v) { return moneda.formatear(v); }

    private static String tasaTexto(Moneda m) {
        return "1 " + m.name() + " = Q" + TasasCambio.quetzalesPor(m).toPlainString();
    }


    private void refrescarMoneda() {
        boolean otra = moneda != Moneda.GTQ;
        BigDecimal pago = totalPago();

        if (!otra) {
            lblTasa.setText("Moneda base del sistema. Elige otra para ver cuánto pagarías en ella.");
        } else {
            String origen = TasasCambio.enLinea()
                    ? "tasa del día" + (TasasCambio.actualizadoEl().isBlank() ? "" : " (" + TasasCambio.actualizadoEl() + ")")
                    : "tasa referencial (sin conexión a internet)";
            lblTasa.setText(tasaTexto(moneda) + " · " + origen
                    + ". La compra se registra en quetzales.");
        }

        filaMoneda.setVisible(otra);
        filaMoneda.setManaged(otra);
        lblNombreMoneda.setText("Total en " + moneda.name());
        lblTotalMoneda.setText(fmt(pago));
        btnPagar.setText("Realizar pago · " + fmt(pago));

        lblRecibidoMoneda.setText("Monto recibido (" + (otra ? moneda.name() : "Q") + ")");
        lblTotalCobrar.setText(fmt(pago));
        tfRecibido.setText(pago.toPlainString());         
        actualizarCambio();
    }

    private void pintarResumen() {
        lineasResumen.getChildren().clear();
        for (LineaCompra l : FlujoCompra.lineas()) {
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
        BigDecimal total = FlujoCompra.total();
        lblBase.setText(Impuestos.q(Impuestos.base(total)));
        lblIva.setText(Impuestos.q(Impuestos.iva(total)));
        lblTotal.setText(Impuestos.q(total));
        btnPagar.setText("Realizar pago · " + fmt(totalPago()));
    }


    private String metodo() {
        if (tgEfectivo.isSelected()) return "efectivo";
        if (tgTarjeta.isSelected()) return "tarjeta";
        return "transferencia";
    }

    private void cambiarMetodo() {
        String m = metodo();
        mostrar(panelEfectivo, m.equals("efectivo"));
        mostrar(panelTarjeta, m.equals("tarjeta"));
        mostrar(panelTransferencia, m.equals("transferencia"));
        mensaje(null);
    }

    private static void mostrar(Node n, boolean ver) {
        n.setVisible(ver);
        n.setManaged(ver);
    }


    @FXML
    private void onExacto() {
        tfRecibido.setText(totalPago().toPlainString());
    }

    private BigDecimal recibido() {
        String t = tfRecibido.getText() == null ? "" : tfRecibido.getText().trim();
        if (t.isEmpty()) return null;
        try {
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void actualizarCambio() {
        BigDecimal r = recibido();
        BigDecimal total = totalPago();
        if (r == null) lblCambio.setText("—");
        else if (r.compareTo(total) >= 0) lblCambio.setText(fmt(r.subtract(total)));
        else lblCambio.setText("Faltan " + fmt(total.subtract(r)));
    }


    private void enlazarCampos() {
        tfRecibido.textProperty().addListener((o, a, b) -> {
            if (b != null && !b.matches("\\d{0,7}(\\.\\d{0,2})?")) { tfRecibido.setText(a); return; }
            actualizarCambio();
            limpiar(errRecibido, tfRecibido);
        });


        tfNumero.textProperty().addListener((o, a, b) -> {
            String f = ValidadorPago.agruparNumero(b);
            if (!f.equals(b)) { tfNumero.setText(f); return; }
            limpiar(errNumero, tfNumero);
        });

        tfVence.textProperty().addListener((o, a, b) -> {
            String f = ValidadorPago.formatearVencimiento(b);
            if (!f.equals(b)) { tfVence.setText(f); return; }
            limpiar(errVence, tfVence);
        });
        tfTitular.textProperty().addListener((o, a, b) -> limpiar(errTitular, tfTitular));
        pfCvv.textProperty().addListener((o, a, b) -> {
            String d = ValidadorPago.soloDigitos(b);
            if (d.length() > 4) d = d.substring(0, 4);
            if (!d.equals(b)) { pfCvv.setText(d); return; }
            limpiar(errCvv, pfCvv);
        });
        tfBoleta.textProperty().addListener((o, a, b) -> {
            String d = ValidadorPago.soloDigitos(b);
            if (d.length() > 14) d = d.substring(0, 14);
            if (!d.equals(b)) { tfBoleta.setText(d); return; }
            limpiar(errBoleta, tfBoleta);
        });
        cbBanco.valueProperty().addListener((o, a, b) -> limpiar(errBanco, cbBanco));
        tfClienteNombre.textProperty().addListener((o, a, b) -> { if (b != null && b.length() > 60) tfClienteNombre.setText(a); });

        tfClienteNit.textProperty().addListener((o, a, b) -> {
            if (b == null) return;
            String n = b.toUpperCase().replaceAll("[^0-9K]", "");
            if (n.length() > 9) n = n.substring(0, 9);
            if (!n.equals(b)) tfClienteNit.setText(n);
        });
        tfClienteCorreo.textProperty().addListener((o, a, b) -> { if (b != null && b.length() > 80) tfClienteCorreo.setText(a); limpiar(errCorreo, tfClienteCorreo); });
    }

    private boolean validar() {
        boolean ok = true;
        if (vendedor && !tfClienteCorreo.getText().isBlank() && !CorreoCompra.correoValido(tfClienteCorreo.getText())) {
            error(errCorreo, "Correo no válido (ej. comprador@gmail.com). Déjalo vacío si no quiere recibirlo.", tfClienteCorreo);
            ok = false;
        }
        switch (metodo()) {
            case "efectivo" -> {
                BigDecimal r = recibido();
                if (r == null || r.compareTo(totalPago()) < 0) {
                    error(errRecibido, "El monto recibido debe cubrir el total (" + fmt(totalPago()) + ").", tfRecibido);
                    ok = false;
                }
            }
            case "tarjeta" -> {
                if (tfTitular.getText() == null || tfTitular.getText().trim().length() < 3) { error(errTitular, "Escribe el nombre como aparece en la tarjeta.", tfTitular); ok = false; }
                if (!ValidadorPago.numeroValido(tfNumero.getText())) { error(errNumero, "Escribe el número de tarjeta (13 a 16 dígitos).", tfNumero); ok = false; }
                if (!ValidadorPago.vencimientoValido(tfVence.getText(), YearMonth.now())) { error(errVence, "Fecha vencida o inválida (MM/AA).", tfVence); ok = false; }
                if (!ValidadorPago.cvvValido(pfCvv.getText())) { error(errCvv, "3 o 4 dígitos.", pfCvv); ok = false; }
            }
            default -> {
                if (cbBanco.getValue() == null) { error(errBanco, "Elige el banco desde el que transferiste.", cbBanco); ok = false; }
                if (tfBoleta.getText() == null || tfBoleta.getText().length() < 6) { error(errBoleta, "Escribe el número de boleta (mínimo 6 dígitos).", tfBoleta); ok = false; }
            }
        }
        return ok;
    }


    @FXML
    private void onPagar() {
        mensaje(null);
        if (!validar()) return;
        btnPagar.setDisable(true);
        overlay.setVisible(true);
 
        PauseTransition espera = new PauseTransition(Duration.millis(metodo().equals("efectivo") ? 500 : 1500));
        espera.setOnFinished(e -> enviar());
        espera.play();
    }

    private void enviar() {
        var usuario = Sesion.usuario();
        if (usuario == null || usuario.idUsuario() == null) {
            terminarConError("Tu sesión expiró. Vuelve a iniciar sesión.");
            return;
        }
        String metodo = metodo();
        BigDecimal total = FlujoCompra.total();
        BigDecimal pago = totalPago();                      
        final Moneda monedaPago = moneda;
        String base;
        switch (metodo) {
            case "efectivo" -> {
                BigDecimal r = recibido() == null ? pago : recibido();
                base = "Efectivo · Recibido " + monedaPago.formatear(r) + " · Cambio " + monedaPago.formatear(r.subtract(pago).max(BigDecimal.ZERO));
            }
            case "tarjeta" -> base = ValidadorPago.marca(tfNumero.getText()) + " •••• " + ultimos4(tfNumero.getText());
            default -> base = cbBanco.getValue() + " · Boleta " + tfBoleta.getText() + " · " + referencia;
        }
    
        final String detalle = monedaPago == Moneda.GTQ ? base
                : base + " · Pagado en " + monedaPago.name() + ": " + monedaPago.formatear(pago) + " (" + tasaTexto(monedaPago) + ")";

        List<DetalleVenta> detalles = new ArrayList<>();
        for (LineaCompra l : FlujoCompra.lineas()) detalles.add(DetalleVenta.nuevo(l.idInventario(), l.cantidad(), l.precio()));
        Venta req = new Venta(null, usuario.idUsuario(), null, total, metodo, detalles);


        String nombreFact = tfClienteNombre.getText();
        if (!vendedor && (nombreFact == null || nombreFact.isBlank()) && usuario.nombre() != null) nombreFact = usuario.nombre();
        String correoFact = vendedor ? tfClienteCorreo.getText() : (usuario.correo() == null ? "" : usuario.correo());
        final String nombreFinal = nombreFact;

        ventaService.registrar(req).whenComplete((venta, ex) -> Platform.runLater(() -> {
            if (ex != null) { terminarConError(textoError(ex)); return; }
            if (venta == null || venta.idVenta() == null) { terminarConError("El servidor no devolvió el número de orden. Revisa las ventas antes de reintentar."); return; }
            FlujoCompra.pagado(venta, metodo, detalle);
            FlujoCompra.datosCliente(nombreFinal, tfClienteNit.getText(), correoFact);
            ClienteUI.ir("cliente-factura");
        }));
    }

    private static String ultimos4(String numero) {
        String d = ValidadorPago.soloDigitos(numero);
        return d.length() <= 4 ? d : d.substring(d.length() - 4);
    }

    private String textoError(Throwable t) {
        while (t instanceof CompletionException && t.getCause() != null) t = t.getCause();
        if (t instanceof ApiException ae) {
            if (ae.getStatus() == 401 || ae.getStatus() == 403) return "Tu sesión no tiene permiso para vender. Inicia sesión de nuevo.";
            if (ae.getStatus() == 409) return ae.getMessage() + " Vuelve atrás y ajusta tu selección.";
        }
        return ClienteUI.mensajeDe(t);
    }

    private void terminarConError(String texto) {
        overlay.setVisible(false);
        btnPagar.setDisable(false);
        mensaje(texto);
    }

    @FXML
    private void onVolver() {
        ClienteUI.ir("cliente-boletos");
    }

    private void mensaje(String texto) {
        boolean v = texto != null && !texto.isEmpty();
        lblMensaje.setText(v ? texto : "");
        lblMensaje.setVisible(v);
        lblMensaje.setManaged(v);
    }

    private void error(Label etiqueta, String texto, Control campo) {
        etiqueta.setText(texto);
        etiqueta.setVisible(true);
        etiqueta.setManaged(true);
        if (!campo.getStyleClass().contains("invalid")) campo.getStyleClass().add("invalid");
    }

    private void limpiar(Label etiqueta, Control campo) {
        etiqueta.setVisible(false);
        etiqueta.setManaged(false);
        campo.getStyleClass().remove("invalid");
    }
}