package com.sistematickets.Controller;

import com.sistematickets.App;
import com.sistematickets.Model.Concierto;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.concurrent.CompletionException;

public final class ClienteUI {
    private ClienteUI() {}

    public static final Locale ES = Locale.of("es", "GT");
    private static Image placeholder;


    public static boolean esVendedor() {
        var u = Sesion.usuario();
        return u != null && u.rol() != null && u.rol().equalsIgnoreCase("vendedor");
    }

    public static String inicio() {
        return esVendedor() ? "interfazmenuVendedor" : "cliente-catalogo";
    }


    public static void ir(String vista) {
        try {
            App.setRoot(vista);
            App.asegurarTamanio(1100, 720);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void cerrarSesion() {
        Sesion.cerrar();
        FlujoCompra.reset();
        ir("seleccion-usuario");
    }

    public static void encabezado(HBox barra, int paso) {
        barra.getChildren().clear();
        if (!barra.getStyleClass().contains("cli-header")) barra.getStyleClass().add("cli-header");
        barra.setAlignment(Pos.CENTER_LEFT);

        Button marca = new Button("TICKETS");
        marca.getStyleClass().add("cli-brand");
        marca.setOnAction(e -> ir(inicio()));

        Region izq = new Region();
        HBox.setHgrow(izq, Priority.ALWAYS);
        Region der = new Region();
        HBox.setHgrow(der, Priority.ALWAYS);

        HBox pasos = new HBox(10);
        pasos.setAlignment(Pos.CENTER);
        String[] nombres = {"Concierto", "Boletos", "Pago"};
        for (int i = 0; i < nombres.length; i++) {
            if (i > 0) {
                Region linea = new Region();
                linea.getStyleClass().add("cli-step-line");
                pasos.getChildren().add(linea);
            }
            int n = i + 1;
            Label punto = new Label(n < paso ? "✓" : String.valueOf(n));
            punto.getStyleClass().add("cli-step-dot");
            punto.getStyleClass().add(n == paso ? "on" : n < paso ? "done" : "off");
            Label texto = new Label(nombres[i]);
            texto.getStyleClass().add("cli-step-text");
            if (n == paso) texto.getStyleClass().add("on");
            pasos.getChildren().addAll(punto, texto);
        }

        var u = Sesion.usuario();
        Label usuario = new Label(u == null || u.nombre() == null ? "" : u.nombre());
        usuario.getStyleClass().add("cli-user");
        Button salir = new Button("Salir");
        salir.getStyleClass().add("tbl-btn");
        salir.setOnAction(e -> cerrarSesion());

        barra.getChildren().addAll(marca, izq);
        if (esVendedor()) {
            Label rol = new Label("VENDEDOR");
            rol.getStyleClass().add("cli-chip");
            barra.getChildren().add(1, rol);
        }
        if (paso > 0) barra.getChildren().add(pasos);
        barra.getChildren().addAll(der, usuario, salir);
    }

    public static Image placeholder() {
        if (placeholder == null) {
            var in = ClienteUI.class.getResourceAsStream("/sinfoto/sin-foto.jpg");
            if (in != null) placeholder = new Image(in);
        }
        return placeholder;
    }

    public static void recortar(ImageView iv, Image img) {
        double w = img.getWidth(), h = img.getHeight();
        double fw = iv.getFitWidth(), fh = iv.getFitHeight();
        if (w <= 0 || h <= 0 || fw <= 0 || fh <= 0) return;
        double ratioVista = fw / fh, ratioImg = w / h;
        if (ratioImg > ratioVista) {
            double nuevoAncho = h * ratioVista;
            iv.setViewport(new Rectangle2D((w - nuevoAncho) / 2, 0, nuevoAncho, h));
        } else {
            double nuevoAlto = w / ratioVista;
            iv.setViewport(new Rectangle2D(0, (h - nuevoAlto) / 2, w, nuevoAlto));
        }
    }

    public static LocalDateTime fecha(Concierto c) {
        return c == null ? null : AdminEntidades.aLocal(c.fechaConcierto());
    }

    public static String mesCorto(LocalDateTime d) {
        String m = d.getMonth().getDisplayName(TextStyle.SHORT, ES).replace(".", "");
        return m.toUpperCase(ES);
    }

    public static String fechaLarga(LocalDateTime d) {
        if (d == null) return "Fecha por confirmar";
        String s = d.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, yyyy · HH:mm", ES));
        return s.substring(0, 1).toUpperCase(ES) + s.substring(1);
    }

    public static String mensajeDe(Throwable t) {
        while (t instanceof CompletionException && t.getCause() != null) t = t.getCause();
        String m = t.getMessage();
        return m == null || m.isBlank() ? "Ocurrió un error inesperado." : m;
    }
}
