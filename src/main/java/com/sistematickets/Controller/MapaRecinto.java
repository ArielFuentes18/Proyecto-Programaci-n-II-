package com.sistematickets.Controller;

import com.sistematickets.Model.InventarioBoletos;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Mapa simulado del recinto: escenario arriba y una franja por localidad (la más cara, más cerca del escenario).
 * Funciona con cualquier número de localidades. Clic en una zona → se elige esa localidad.
 */
public final class MapaRecinto extends StackPane {

    private static final double W = 640, H = 440;
    /** De la zona más cercana al escenario (más cara) a la más lejana. */
    private static final String[] PALETA = {"#e879f9", "#c084fc", "#a78bfa", "#8b5cf6", "#6d28d9", "#4c1d95"};
    private static final String AGOTADO = "#3b3550";

    private final Pane lienzo = new Pane();
    /** Las zonas y sus textos viven en capas distintas: los textos siempre quedan encima sin reordenar nada. */
    private final Group capaZonas = new Group();
    private final Group capaTextos = new Group();
    private final Map<Integer, Polygon> zonas = new LinkedHashMap<>();
    private final Map<Integer, Color> colores = new LinkedHashMap<>();
    private final Map<Integer, Boolean> agotadas = new LinkedHashMap<>();

    public MapaRecinto(List<InventarioBoletos> inventario, Consumer<Integer> alElegir) {
        setMinSize(0, 0);
        lienzo.setMinSize(W, H);
        lienzo.setPrefSize(W, H);
        lienzo.setMaxSize(W, H);
        getChildren().add(lienzo);
        setAlignment(Pos.CENTER);
        widthProperty().addListener((o, a, b) -> escalar());
        heightProperty().addListener((o, a, b) -> escalar());

        dibujarEscenario();
        capaTextos.setMouseTransparent(true);
        lienzo.getChildren().addAll(capaZonas, capaTextos);
        if (inventario == null || inventario.isEmpty()) {
            Label vacio = new Label("Este concierto aún no tiene localidades.");
            vacio.getStyleClass().add("cli-subtitle");
            getChildren().setAll(vacio);
            return;
        }
        dibujarZonas(inventario, alElegir);
    }

    public static String colorDe(int posicion) {
        return PALETA[Math.min(Math.max(posicion, 0), PALETA.length - 1)];
    }

    // ---------- dibujo ----------
    private void dibujarEscenario() {
        Rectangle escenario = new Rectangle(W / 2 - 150, 14, 300, 44);
        escenario.setArcWidth(18);
        escenario.setArcHeight(18);
        escenario.setFill(Color.web("#ede9fe", 0.14));
        escenario.setStroke(Color.web("#c4b5fd", 0.55));
        Text t = new Text("ESCENARIO");
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        t.setFill(Color.web("#ede9fe"));
        t.setX(W / 2 - t.getLayoutBounds().getWidth() / 2);
        t.setY(41);
        t.setMouseTransparent(true);
        lienzo.getChildren().addAll(escenario, t);
    }

    private void dibujarZonas(List<InventarioBoletos> inventario, Consumer<Integer> alElegir) {
        List<InventarioBoletos> orden = new ArrayList<>(inventario);
        orden.sort(Comparator.comparing(InventarioBoletos::precio, Comparator.nullsLast(Comparator.reverseOrder())));

        int n = orden.size();
        double y0 = 86, alto = 330, hueco = 8;
        double h = (alto - hueco * (n - 1)) / n;
        double cx = W / 2;

        for (int i = 0; i < n; i++) {
            InventarioBoletos inv = orden.get(i);
            double yArriba = y0 + i * (h + hueco);
            double yAbajo = yArriba + h;
            double mitadArriba = semiancho(yArriba, y0, alto);
            double mitadAbajo = semiancho(yAbajo, y0, alto);

            Polygon zona = new Polygon(
                    cx - mitadArriba, yArriba, cx + mitadArriba, yArriba,
                    cx + mitadAbajo, yAbajo, cx - mitadAbajo, yAbajo);
            boolean agotada = inv.disponibles() == 0;
            Color color = Color.web(agotada ? AGOTADO : colorDe(i * (PALETA.length - 1) / Math.max(1, n - 1)));
            zona.setFill(color);
            zona.setStroke(Color.web("#ffffff", 0.18));
            zona.setStrokeWidth(1);
            zona.setCursor(agotada ? Cursor.DEFAULT : Cursor.HAND);

            Integer id = inv.idInventario();
            zonas.put(id, zona);
            colores.put(id, color);
            agotadas.put(id, agotada);

            if (!agotada) {
                zona.setOnMouseEntered(e -> zona.setFill(color.deriveColor(0, 1, 0.9, 1)));   // un poco más oscuro, sin brillo
                zona.setOnMouseExited(e -> zona.setFill(color));
                zona.setOnMouseClicked(e -> alElegir.accept(id));
            }
            capaZonas.getChildren().add(zona);

            double yCentro = (yArriba + yAbajo) / 2;
            Text nombre = new Text(inv.localidad());
            nombre.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            nombre.setFill(Color.WHITE);
            nombre.setX(cx - nombre.getLayoutBounds().getWidth() / 2);
            nombre.setY(yCentro - 2);
            nombre.setMouseTransparent(true);

            String linea2 = agotada ? "Agotado" : Impuestos.q(inv.precio());
            Text detalle = new Text(linea2);
            detalle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
            detalle.setFill(Color.web("#ffffff", 0.92));
            detalle.setX(cx - detalle.getLayoutBounds().getWidth() / 2);
            detalle.setY(yCentro + 16);
            detalle.setMouseTransparent(true);
            capaTextos.getChildren().addAll(nombre, detalle);
        }
    }

    /** La sala se ensancha hacia el fondo. */
    private static double semiancho(double y, double y0, double alto) {
        return 120 + (y - y0) / alto * 190;
    }

    // ---------- estado visual ----------
    /** @param conBoletos localidades con cantidad &gt; 0;  @param activa la localidad que se está editando (puede ser null) */
    public void actualizar(Set<Integer> conBoletos, Integer activa) {
        for (Map.Entry<Integer, Polygon> e : zonas.entrySet()) {
            Integer id = e.getKey();
            Polygon zona = e.getValue();
            boolean elegida = conBoletos.contains(id);
            boolean esActiva = id.equals(activa);
            zona.setStroke(esActiva ? Color.WHITE : elegida ? Color.web("#ffffff", 0.85) : Color.web("#ffffff", 0.18));
            zona.setStrokeWidth(esActiva ? 3.5 : elegida ? 2 : 1);
            zona.setEffect(null);
            if (esActiva) zona.toFront();          // solo reordena dentro de capaZonas
        }
    }

    private void escalar() {
        double s = Math.min(getWidth() / W, getHeight() / H);
        if (s <= 0 || Double.isNaN(s)) return;
        s = Math.min(s, 1.4);
        lienzo.setScaleX(s);
        lienzo.setScaleY(s);
    }
}
