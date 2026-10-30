package com.sistematickets.Controller;   
import com.sistematickets.Model.Concierto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

public class TarjetaConciertoController {


    private static final String BASE_FOTOS =
            "https://runmfjrhshpxzroyqohi.supabase.co/storage/v1/object/public/fotos-artistas/";

    
    private static final String PLACEHOLDER = "/sinfoto/sin-foto.jpg";   

    @FXML private Label lblFecha;
    @FXML private Label lblNombre;
    @FXML private Label lblLugar;
    @FXML private Button btnFoto;
    @FXML private ImageView imgArtista;

    private Concierto concierto;
    private Consumer<Concierto> alSeleccionar;

    @FXML
    private void initialize() {
   
        Rectangle clip = new Rectangle(imgArtista.getFitWidth(), imgArtista.getFitHeight());
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        imgArtista.setClip(clip);
    }


    public void setDatos(Concierto c, Consumer<Concierto> alSeleccionar) {
        this.concierto = c;
        this.alSeleccionar = alSeleccionar;

        
        lblNombre.setText(c.nombreArtista());
        ponerImagen(construirUrlImagen(c.imagenPath()));
     
        lblLugar.setText(c.recinto() != null ? c.recinto() : "");

        LocalDate fecha = parsearFecha(c.fechaConcierto());
        lblFecha.setText(fecha != null ? formatearFecha(fecha)
                                       : (c.fechaConcierto() != null ? c.fechaConcierto() : ""));

        if (c.tituloEvento() != null && !c.tituloEvento().isBlank()) {
            Tooltip.install(btnFoto, new Tooltip(c.tituloEvento()));
        }
    }

    @FXML
    private void abrirVenta() {
        if (alSeleccionar != null && concierto != null) {
            alSeleccionar.accept(concierto);   
        }
    }

    private LocalDate parsearFecha(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            LocalDateTime dt = AdminEntidades.aLocal(s);
            if (dt != null) return dt.toLocalDate();
        } catch (RuntimeException ignorado) {
        }
        try {
            if (s.length() <= 10) return LocalDate.parse(s);
            try {
                return OffsetDateTime.parse(s).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
            } catch (DateTimeParseException e) {
                return LocalDateTime.parse(s.replace(' ', 'T')).toLocalDate();
            }
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(s.substring(0, 10));
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private String formatearFecha(LocalDate f) {
        String mes = f.getMonth().getDisplayName(TextStyle.FULL, Locale.of("es", "GT"));
        mes = mes.substring(0, 1).toUpperCase() + mes.substring(1);
        return f.getDayOfMonth() + " " + mes;
    }


    private String construirUrlImagen(String imagenPath) {
        if (imagenPath == null || imagenPath.isBlank()) return null;
        if (imagenPath.startsWith("http")) return imagenPath;  
        return BASE_FOTOS + imagenPath;
    }

private void ponerImagen(String url) {
    System.out.println("[Tarjeta] " + lblNombre.getText() + " -> imagen: " + url);
    if (url == null || url.isBlank()) {
        ponerPlaceholder();
        return;
    }
   
    String segura = url.replaceAll("\\s+", "");

    Image img;
    try {
        img = new Image(segura, true);
    } catch (IllegalArgumentException e) {
        System.out.println("[Tarjeta] URL inválida: " + segura);
        ponerPlaceholder();
        return;
    }
    imgArtista.setImage(img);

    if (img.getProgress() >= 1.0) {
        recortarCentrado(img);
    } else {
        img.progressProperty().addListener((obs, viejo, nuevo) -> {
            if (nuevo.doubleValue() >= 1.0 && !img.isError()) recortarCentrado(img);
        });
    }
    img.errorProperty().addListener((obs, viejo, hayError) -> {
        if (hayError) {
            System.out.println("[Tarjeta] ERROR cargando " + segura + " : " + img.getException());
            ponerPlaceholder();
        }
    });
}

    private void ponerPlaceholder() {
        var in = getClass().getResourceAsStream(PLACEHOLDER);
        if (in != null) {
            Image p = new Image(in);
            imgArtista.setImage(p);
            recortarCentrado(p);
        }
    }

    private void recortarCentrado(Image img) {
        double w = img.getWidth();
        double h = img.getHeight();
        if (w <= 0 || h <= 0) return;

        double ratioVista = imgArtista.getFitWidth() / imgArtista.getFitHeight();
        double ratioImg = w / h;

        if (ratioImg > ratioVista) {        
            double nuevoAncho = h * ratioVista;
            imgArtista.setViewport(new Rectangle2D((w - nuevoAncho) / 2, 0, nuevoAncho, h));
        } else {                            
            double nuevoAlto = w / ratioVista;
            imgArtista.setViewport(new Rectangle2D(0, (h - nuevoAlto) / 2, w, nuevoAlto));
        }
    }
}