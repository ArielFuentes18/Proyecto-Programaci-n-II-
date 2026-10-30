package com.sistematickets.Controller;

import com.sistematickets.Model.Concierto;
import com.sistematickets.Model.Venta;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class FacturaClienteController {

    @FXML private HBox encabezado;
    @FXML private Label lblTituloVista;
    @FXML private Label lblSubtituloVista;
    @FXML private VBox papel;
    @FXML private Label lblFactura;
    @FXML private Label lblFecha;
    @FXML private Label lblCliente;
    @FXML private Label lblCorreo;
    @FXML private Label lblAtendidoTit;
    @FXML private Label lblAtendido;
    @FXML private Label lblEvento;
    @FXML private Label lblArtista;
    @FXML private Label lblFechaEvento;
    @FXML private Label lblRecinto;
    @FXML private GridPane tablaLineas;
    @FXML private Label lblBase;
    @FXML private Label lblIva;
    @FXML private Label lblTotal;
    @FXML private Label lblMetodo;
    @FXML private Label lblDetallePago;
    @FXML private Canvas canvasQr;
    @FXML private Label lblCodigo;
    @FXML private Label lblPdfEstado;
    @FXML private Label lblCorreoEstado;
    @FXML private Button btnSeguir;

    private FacturaDatos datos;

    @FXML
    private void initialize() {
        ClienteUI.encabezado(encabezado, 4);
        Venta v = FlujoCompra.venta();
        Concierto c = FlujoCompra.concierto();
        if (v == null || c == null) {
            Platform.runLater(() -> ClienteUI.ir(ClienteUI.inicio()));
            return;
        }
        boolean vendedor = ClienteUI.esVendedor();
        var u = Sesion.usuario();
        String usuarioNombre = u == null || u.nombre() == null ? "" : u.nombre();
        String usuarioCorreo = u == null || u.correo() == null ? "" : u.correo();

        LocalDateTime fecha = AdminEntidades.aLocal(v.fechaVenta());
        if (fecha == null) fecha = LocalDateTime.now();
        int id = v.idVenta();
        BigDecimal total = v.totalVenta() != null ? v.totalVenta() : FlujoCompra.total();   //

        String metodo = switch (FlujoCompra.metodoPago()) {
            case "efectivo" -> "Efectivo";
            case "tarjeta" -> "Tarjeta de crédito / débito";
            default -> "Transferencia bancaria";
        };

        datos = new FacturaDatos(
                String.format("FAC-%d-%06d", fecha.getYear(), id),
                fecha.format(DateTimeFormatter.ofPattern("d 'de' MMMM yyyy, HH:mm", ClienteUI.ES)),
                FlujoCompra.clienteNombre(),
                "NIT: " + FlujoCompra.clienteNit() + (vendedor || usuarioCorreo.isBlank() ? "" : " · " + usuarioCorreo),
                vendedor ? usuarioNombre : null,
                c.tituloEvento() == null ? "" : c.tituloEvento(),
                c.nombreArtista() == null ? "" : c.nombreArtista(),
                ClienteUI.fechaLarga(ClienteUI.fecha(c)),
                c.recinto() == null ? "" : c.recinto(),
                FlujoCompra.lineas(),
                total,
                metodo,
                FlujoCompra.detallePago(),
                String.format("TKT-%06d-%d", id, c.idConcierto()));

        pintar();

        if (vendedor) {
            lblTituloVista.setText("¡Venta registrada con éxito!");
            lblSubtituloVista.setText("Entrega la factura al cliente: puedes imprimirla o descargarla en PDF.");
            btnSeguir.setText("Nueva venta");
        }

        enviarCorreos(vendedor ? FlujoCompra.clienteCorreo() : usuarioCorreo);
    }

    private void enviarCorreos(String destino) {
        if (!CorreoCompra.correoValido(destino)) {
            if (destino != null && !destino.isBlank()) avisoCorreo("No se enviaron correos: la dirección \"" + destino + "\" no es válida.");
            return;                                                   
        }
        if (!FlujoCompra.marcarCorreoProcesado()) return;              
        final FacturaDatos d = datos;
        final String para = destino.trim();
        final int cuantos = 1 + d.boletos().size();
        avisoCorreo("Enviando " + cuantos + " correos a " + para + "…");

        CompletableFuture.supplyAsync(() -> {
            try {
                return CorreoCompra.enviar(d, para, FacturaPdf.generar(d));
            } catch (Exception ex) {
                return new CorreoCompra.Resultado(cuantos, 0, java.util.List.of(String.valueOf(ex.getMessage())), true);
            }
        }).thenAccept(r -> Platform.runLater(() -> {
            if (!r.configurado()) {
                avisoCorreo("Compra registrada, pero el envío de correos no está configurado (falta usuario/clave en correo.properties).");
            } else if (r.todoOk()) {
                avisoCorreo("✔ Enviamos la factura y " + (cuantos - 1) + " ticket(s) por separado a " + para + ".");
            } else {
                avisoCorreo("Se enviaron " + r.enviados() + " de " + r.esperados() + " correos a " + para + ". Problema: " + r.errores().get(0));
                r.errores().forEach(e -> System.err.println("[correo] " + e));
            }
        }));
    }

    private void avisoCorreo(String texto) {
        lblCorreoEstado.setText(texto);
        lblCorreoEstado.setVisible(true);
        lblCorreoEstado.setManaged(true);
    }

    private void pintar() {
        FacturaDatos d = datos;
        lblFactura.setText(d.numero());
        lblFecha.setText(d.fechaTexto());
        lblCliente.setText(d.clienteNombre());
        lblCorreo.setText(d.clienteDoc());
        boolean atendido = d.atendidoPor() != null && !d.atendidoPor().isBlank();
        for (var n : new javafx.scene.Node[]{lblAtendidoTit, lblAtendido}) { n.setVisible(atendido); n.setManaged(atendido); }
        if (atendido) lblAtendido.setText(d.atendidoPor());

        lblEvento.setText(d.evento());
        lblArtista.setText(d.artista());
        lblFechaEvento.setText(d.fechaEvento());
        lblRecinto.setText(d.recinto());

        pintarLineas(d);
        lblBase.setText(Impuestos.q(d.base()));
        lblIva.setText(Impuestos.q(d.iva()));
        lblTotal.setText(Impuestos.q(d.total()));
        lblMetodo.setText(d.metodo());
        lblDetallePago.setText(d.detallePago());
        lblCodigo.setText(d.codigo());
        dibujarQr(d.codigo());
    }

    private void pintarLineas(FacturaDatos d) {
        tablaLineas.getChildren().clear();
        tablaLineas.getColumnConstraints().clear();
        double[] pesos = {46, 14, 20, 20};
        for (int i = 0; i < pesos.length; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(pesos[i]);
            if (i > 0) cc.setHalignment(HPos.RIGHT);
            tablaLineas.getColumnConstraints().add(cc);
        }
        String[] cab = {"Localidad", "Cant.", "Precio", "Subtotal"};
        for (int i = 0; i < cab.length; i++) tablaLineas.add(celda(cab[i], "cli-papel-th"), i, 0);

        int fila = 1;
        for (LineaCompra l : d.lineas()) {
            tablaLineas.add(celda(l.localidad(), "cli-papel-td"), 0, fila);
            tablaLineas.add(celda(String.valueOf(l.cantidad()), "cli-papel-td"), 1, fila);
            tablaLineas.add(celda(Impuestos.q(l.precio()), "cli-papel-td"), 2, fila);
            tablaLineas.add(celda(Impuestos.q(l.subtotal()), "cli-papel-td"), 3, fila);
            fila++;
        }
    }

    private static Label celda(String texto, String clase) {
        Label l = new Label(texto);
        l.getStyleClass().add(clase);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private void dibujarQr(String texto) {
        boolean[][] m = com.sistematickets.Service.QrCodigo.modulos(texto);
        GraphicsContext g = canvasQr.getGraphicsContext2D();
        double lado = canvasQr.getWidth();
        int n = m.length;
        double modulo = lado / (n + 2);                
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, lado, lado);
        g.setFill(Color.web("#1f1b2e"));
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                if (m[y][x]) g.fillRect((x + 1) * modulo, (y + 1) * modulo, Math.ceil(modulo), Math.ceil(modulo));
            }
        }
    }

    // ---------- PDF ----------
    @FXML
    private void onDescargarPdf() {
        if (datos == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar factura en PDF");
        fc.setInitialFileName(datos.nombreArchivo());
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));
        File descargas = new File(System.getProperty("user.home"), "Downloads");
        if (descargas.isDirectory()) fc.setInitialDirectory(descargas);

        File destino = fc.showSaveDialog(papel.getScene().getWindow());
        if (destino == null) return;                                          
        if (!destino.getName().toLowerCase().endsWith(".pdf")) destino = new File(destino.getParentFile(), destino.getName() + ".pdf");
        try {
            FacturaPdf.guardar(datos, destino);
            avisoPdf("PDF guardado en: " + destino.getAbsolutePath());
        } catch (IOException | RuntimeException ex) {
            ex.printStackTrace();
            avisoPdf("No se pudo guardar el PDF: " + ex.getMessage());
        }
    }

    private void avisoPdf(String texto) {
        lblPdfEstado.setText(texto);
        lblPdfEstado.setVisible(true);
        lblPdfEstado.setManaged(true);
    }

    // ---------- impresión ----------
    @FXML
    private void onImprimir() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            avisoPdf("No se encontró ninguna impresora. Puedes descargar el PDF e imprimirlo desde allí.");
            return;
        }
        if (!job.showPrintDialog(papel.getScene().getWindow())) return;
        PageLayout pl = job.getJobSettings().getPageLayout();
        double ancho = papel.getBoundsInParent().getWidth();
        Scale escala = null;
        if (ancho > 0 && pl.getPrintableWidth() < ancho) {
            double s = pl.getPrintableWidth() / ancho;
            escala = new Scale(s, s);
            papel.getTransforms().add(escala);
        }
        try {
            if (job.printPage(papel)) job.endJob();
        } finally {
            if (escala != null) papel.getTransforms().remove(escala);
        }
    }

    @FXML
    private void onSeguir() {
        FlujoCompra.limpiarCarrito();
        ClienteUI.ir(ClienteUI.inicio());
    }
}
