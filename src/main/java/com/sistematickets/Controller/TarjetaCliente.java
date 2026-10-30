package com.sistematickets.Controller;

import com.sistematickets.Model.Concierto;
import com.sistematickets.Service.ImagenService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Consumer;

public final class TarjetaCliente {
    private TarjetaCliente() {}

    public static VBox crear(Concierto c, BigDecimal desde, int disponibles, boolean destacada, Consumer<Concierto> alElegir) {
        double w = destacada ? 320 : 262;
        double h = destacada ? 200 : 158;
        boolean comprable = c.vendible() && desde != null && disponibles > 0;


        ImageView iv = new ImageView();
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setPreserveRatio(false);
        StackPane foto = new StackPane(iv);
        foto.setMinSize(w, h);
        foto.setPrefSize(w, h);
        foto.setMaxSize(w, h);
        Rectangle clip = new Rectangle(w, h + 40);          
        clip.setArcWidth(40);
        clip.setArcHeight(40);
        foto.setClip(clip);
        ImagenService.getInstance().mostrar(iv, c.imagenPath(), ClienteUI.placeholder(), img -> ClienteUI.recortar(iv, img));

        LocalDateTime f = ClienteUI.fecha(c);
        if (f != null) {
            Label dia = new Label(String.valueOf(f.getDayOfMonth()));
            dia.getStyleClass().add("cli-fecha-dia");
            Label mes = new Label(ClienteUI.mesCorto(f));
            mes.getStyleClass().add("cli-fecha-mes");
            VBox insignia = new VBox(0, dia, mes);
            insignia.setAlignment(Pos.CENTER);
            insignia.getStyleClass().add("cli-fecha-badge");
            insignia.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            StackPane.setAlignment(insignia, Pos.TOP_LEFT);
            StackPane.setMargin(insignia, new Insets(12));
            foto.getChildren().add(insignia);
        }

        String textoChip;
        boolean alerta = false;
        if (!c.vendible()) { textoChip = "No disponible"; alerta = true; }
        else if (desde == null) { textoChip = "Próximamente"; }
        else if (disponibles == 0) { textoChip = "Agotado"; alerta = true; }
        else if (disponibles <= 20) { textoChip = "Últimos boletos"; alerta = true; }
        else { textoChip = c.genero(); }
        if (textoChip != null && !textoChip.isBlank()) {
            Label chip = new Label(textoChip);
            chip.getStyleClass().add("cli-chip");
            if (alerta) chip.getStyleClass().add("alerta");
            StackPane.setAlignment(chip, Pos.TOP_RIGHT);
            StackPane.setMargin(chip, new Insets(12));
            foto.getChildren().add(chip);
        }

        String nombre = c.nombreArtista() == null || c.nombreArtista().isBlank() ? c.tituloEvento() : c.nombreArtista();
        Label lblNombre = new Label(nombre == null ? "" : nombre);
        lblNombre.getStyleClass().add("cli-card-nombre");
        lblNombre.setMaxWidth(w - 32);
        Label lblEvento = new Label(c.tituloEvento() == null ? "" : c.tituloEvento());
        lblEvento.getStyleClass().add("cli-card-evento");
        lblEvento.setMaxWidth(w - 32);
        Label lblLugar = new Label(c.recinto() == null ? "" : c.recinto());
        lblLugar.getStyleClass().add("cli-card-lugar");
        lblLugar.setMaxWidth(w - 32);

        Label pre = new Label(comprable ? "Desde" : " ");
        pre.getStyleClass().add("cli-precio-pre");
        Label precio = new Label(comprable ? Impuestos.q(desde) : "—");
        precio.getStyleClass().add("cli-precio");
        VBox bloquePrecio = new VBox(0, pre, precio);
        Region sep = new Region();
        HBox.setHgrow(sep, Priority.ALWAYS);
        Label accion = new Label(comprable ? "Ver boletos" : "Sin boletos");
        accion.getStyleClass().add("cli-mini-btn");
        if (!comprable) accion.getStyleClass().add("off");
        HBox pie = new HBox(8, bloquePrecio, sep, accion);
        pie.setAlignment(Pos.CENTER_LEFT);

        VBox cuerpo = new VBox(4, lblNombre, lblEvento, lblLugar, pie);
        cuerpo.setPadding(new Insets(14, 16, 16, 16));
        VBox.setMargin(pie, new Insets(10, 0, 0, 0));

        VBox tarjeta = new VBox(foto, cuerpo);
        tarjeta.getStyleClass().add("cli-card");
        if (!comprable) tarjeta.getStyleClass().add("off");
        tarjeta.setMinWidth(w);
        tarjeta.setPrefWidth(w);
        tarjeta.setMaxWidth(w);
        tarjeta.setOnMouseClicked(e -> { if (comprable) alElegir.accept(c); });
        return tarjeta;
    }
}
