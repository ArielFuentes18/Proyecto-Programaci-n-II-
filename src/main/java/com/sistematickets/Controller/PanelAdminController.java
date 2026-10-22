package com.sistematickets.Controller;

import com.sistematickets.App;
import com.sistematickets.Controller.AdminEntidades.Accion;
import com.sistematickets.Controller.AdminEntidades.Campo;
import com.sistematickets.Controller.AdminEntidades.Columna;
import com.sistematickets.Controller.AdminEntidades.Entidad;
import com.sistematickets.Controller.AdminEntidades.Opcion;
import com.sistematickets.Controller.AdminEntidades.Tipo;
import com.sistematickets.Service.ApiException;
import com.sistematickets.Service.CrudService;
import com.sistematickets.Service.StorageService;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

/** Panel de administración: una tabla genérica + formularios CRUD generados desde AdminEntidades. */
public class PanelAdminController {

    @FXML private StackPane root;
    @FXML private StackPane modalHost;
    @FXML private VBox nav;
    @FXML private Label lblTitulo;
    @FXML private Label lblConteo;
    @FXML private Label lblUsuario;
    @FXML private Label lblToast;
    @FXML private TextField tfBuscar;
    @FXML private Button btnNuevo;
    @FXML private TableView<Map<String, Object>> tabla;

    private final CrudService crud = new CrudService();
    private final StorageService storage = new StorageService();
    private final Map<String, Entidad> entidades = AdminEntidades.mapa();
    private final ObservableList<Map<String, Object>> todos = FXCollections.observableArrayList();
    private final FilteredList<Map<String, Object>> filtrados = new FilteredList<>(todos, r -> true);
    private final Map<String, Button> botonesNav = new LinkedHashMap<>();
    private final Label lblVacio = new Label();
    private final Button btnReintentar = new Button("Reintentar");
    private final PauseTransition pausaToast = new PauseTransition(Duration.seconds(2.8));
    private Entidad actual;

    // ---------- inicio ----------
    @FXML
    private void initialize() {
        var u = Sesion.usuario();
        lblUsuario.setText(u == null ? "" : (u.nombre() != null ? u.nombre() : String.valueOf(u.correo())));

        tabla.setItems(filtrados);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        filtrados.addListener((ListChangeListener<Map<String, Object>>) c -> actualizarConteo());
        tfBuscar.textProperty().addListener((o, a, b) -> {
            String q = b == null ? "" : b.toLowerCase().trim();
            filtrados.setPredicate(r -> q.isEmpty() || r.toString().toLowerCase().contains(q));
        });

        lblVacio.getStyleClass().add("muted");
        btnReintentar.getStyleClass().add("tbl-btn");
        btnReintentar.setOnAction(e -> cargar());
        VBox ph = new VBox(10, lblVacio, btnReintentar);
        ph.setAlignment(Pos.CENTER);
        tabla.setPlaceholder(ph);

        modalHost.setOnMouseClicked(e -> { if (e.getTarget() == modalHost) cerrarModal(); });

        entidades.forEach((k, e) -> {
            Button b = new Button(e.titulo());
            b.getStyleClass().add("nav-btn");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(ev -> seleccionar(e));
            botonesNav.put(k, b);
            nav.getChildren().add(b);
        });
        seleccionar(entidades.values().iterator().next());
    }

    @FXML private void onNuevo() { abrirFormulario(null); }

    @FXML
    private void onSalir() {
        Sesion.cerrar();
        irAlSelector();
    }

    private void irAlSelector() {
        try {
            App.setRoot("seleccion-usuario");
        } catch (IOException e) {
            toast("No se pudo volver al selector: " + e.getMessage());
        }
    }

    // ---------- navegación y carga ----------
    private void seleccionar(Entidad e) {
        actual = e;
        botonesNav.forEach((k, b) -> {
            b.getStyleClass().remove("on");
            if (k.equals(e.clave())) b.getStyleClass().add("on");
        });
        lblTitulo.setText(e.titulo());
        btnNuevo.setText("+ " + e.nuevo());
        tfBuscar.clear();
        construirColumnas(e);
        cargar();
    }

    private void cargar() {
        Entidad e = actual;
        lblVacio.setText("Cargando…");
        btnReintentar.setVisible(false);
        btnReintentar.setManaged(false);
        todos.clear();
        List<CompletableFuture<Void>> previos = new ArrayList<>();
        for (String n : e.lookups()) {
            var l = AdminEntidades.LOOKUPS.get(n);
            previos.add(crud.listar(l.endpoint()).thenAccept(r -> AdminEntidades.CATALOGOS.put(n, r)));
        }
        CompletableFuture.allOf(previos.toArray(new CompletableFuture[0]))
                .thenCompose(v -> crud.listar(e.endpoint()))
                .whenComplete((res, ex) -> Platform.runLater(() -> {
                    if (e != actual) return;
                    if (ex != null) {
                        lblVacio.setText(mensaje(ex));
                        btnReintentar.setVisible(true);
                        btnReintentar.setManaged(true);
                    } else {
                        todos.setAll(res.stream().filter(e.filtro()).toList());
                        lblVacio.setText("Aún no hay registros. Usa “" + e.nuevo() + "” para agregar el primero.");
                    }
                    actualizarConteo();
                }));
    }

    private void actualizarConteo() {
        int n = filtrados.size();
        lblConteo.setText(n + (n == 1 ? " registro" : " registros"));
    }

    // ---------- tabla ----------
    private void construirColumnas(Entidad e) {
        tabla.getColumns().clear();
        for (Columna c : e.columnas()) {
            TableColumn<Map<String, Object>, String> col = new TableColumn<>(c.titulo());
            col.setSortable(false);
            col.setCellValueFactory(d -> new SimpleStringProperty(c.valor().apply(d.getValue())));
            col.setCellFactory(tc -> new TableCell<Map<String, Object>, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    Map<String, Object> fila = (!empty && getTableRow() != null) ? getTableRow().getItem() : null;
                    if (fila == null) { setText(null); setGraphic(null); return; }
                    if (c.imagen()) {
                        setText(null);
                        setGraphic(avatar(fila));
                    } else if (c.pill()) {
                        setText(null);
                        Label p = new Label(item);
                        p.getStyleClass().add("pill");
                        setGraphic(p);
                    } else {
                        setGraphic(null);
                        setText(item);
                    }
                }
            });
            tabla.getColumns().add(col);
        }
        TableColumn<Map<String, Object>, Void> acc = new TableColumn<>("Acciones");
        acc.setSortable(false);
        acc.setMinWidth(220);
        acc.setCellFactory(tc -> new TableCell<Map<String, Object>, Void>() {
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                Map<String, Object> fila = (!empty && getTableRow() != null) ? getTableRow().getItem() : null;
                if (fila == null) { setGraphic(null); return; }
                HBox h = new HBox(6);
                h.setAlignment(Pos.CENTER_RIGHT);
                Accion a = e.accion();
                if (a != null && a.visible().test(fila)) h.getChildren().add(boton(a.texto(), false, () -> aprobar(fila)));
                h.getChildren().add(boton("Editar", false, () -> abrirFormulario(fila)));
                h.getChildren().add(boton("Eliminar", true, () -> confirmarEliminar(fila)));
                setGraphic(h);
            }
        });
        tabla.getColumns().add(acc);
    }

    private Button boton(String texto, boolean peligro, Runnable accion) {
        Button b = new Button(texto);
        b.getStyleClass().add("tbl-btn");
        if (peligro) b.getStyleClass().add("danger");
        b.setOnAction(ev -> accion.run());
        return b;
    }

    private Node avatar(Map<String, Object> fila) {
        String url = AdminEntidades.str(fila.get("imagen_url"));
        String nombre = AdminEntidades.str(fila.get("nombre_artistico"));
        StackPane box = new StackPane();
        box.getStyleClass().add("avatar");
        box.setMinSize(44, 44);
        box.setPrefSize(44, 44);
        box.setMaxSize(44, 44);
        Label ini = new Label(nombre.isEmpty() ? "?" : nombre.substring(0, 1).toUpperCase());
        ini.getStyleClass().add("avatar-text");
        box.getChildren().add(ini);
        if (!url.isEmpty()) {
            try {
                Image img = new Image(url, 44, 44, false, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(44);
                iv.setFitHeight(44);
                Rectangle clip = new Rectangle(44, 44);
                clip.setArcWidth(14);
                clip.setArcHeight(14);
                iv.setClip(clip);
                img.errorProperty().addListener((o, a, err) -> { if (err) iv.setVisible(false); });
                box.getChildren().add(iv);
            } catch (IllegalArgumentException ignorado) {
                // URL inválida: se queda la inicial
            }
        }
        return box;
    }

    // ---------- modal ----------
    private void abrirModal(Node card) {
        modalHost.getChildren().setAll(card);
        modalHost.setOpacity(0);
        modalHost.setManaged(true);
        modalHost.setVisible(true);
        FadeTransition f = new FadeTransition(Duration.millis(200), modalHost);
        f.setToValue(1);
        f.play();
    }

    private void cerrarModal() {
        FadeTransition f = new FadeTransition(Duration.millis(160), modalHost);
        f.setToValue(0);
        f.setOnFinished(ev -> {
            modalHost.setVisible(false);
            modalHost.setManaged(false);
            modalHost.getChildren().clear();
        });
        f.play();
    }

    private void toast(String m) {
        lblToast.setText(m);
        lblToast.setManaged(true);
        lblToast.setVisible(true);
        lblToast.setOpacity(0);
        FadeTransition in = new FadeTransition(Duration.millis(250), lblToast);
        in.setToValue(1);
        in.play();
        pausaToast.setOnFinished(e -> {
            FadeTransition out = new FadeTransition(Duration.millis(300), lblToast);
            out.setToValue(0);
            out.setOnFinished(x -> { lblToast.setVisible(false); lblToast.setManaged(false); });
            out.play();
        });
        pausaToast.playFromStart();
    }

    private String mensaje(Throwable t) {
        while (t instanceof CompletionException && t.getCause() != null) t = t.getCause();
        if (t instanceof ApiException ae && ae.getStatus() == 401) {
            Sesion.cerrar();
            irAlSelector();
            return "Tu sesión expiró.";
        }
        return t.getMessage() != null ? t.getMessage() : "Ocurrió un error inesperado.";
    }

    private static void mostrar(Label l, String texto) {
        boolean v = texto != null && !texto.isEmpty();
        l.setText(v ? texto : "");
        l.setVisible(v);
        l.setManaged(v);
    }

    // ---------- formulario (POST / PUT) ----------
    private static final class Entrada {
        final Campo campo;
        final VBox caja = new VBox(6);
        final Label error = new Label();
        Supplier<Object> lector;
        File archivo;

        Entrada(Campo c) {
            campo = c;
            Label l = new Label(c.label() + (c.req() ? " *" : ""));
            l.getStyleClass().add("field-label");
            caja.getChildren().add(l);
            error.getStyleClass().add("err-label");
            error.setWrapText(true);
            error.setVisible(false);
            error.setManaged(false);
        }

        void marcar(String msg) { mostrar(error, msg); }
    }

    private Entrada crearEntrada(Campo c, Object valor) {
        Entrada en = new Entrada(c);
        String vs = AdminEntidades.str(valor);
        switch (c.tipo()) {
            case PASSWORD -> {
                PasswordField pf = new PasswordField();
                pf.getStyleClass().add("login-field");
                en.caja.getChildren().add(pf);
                en.lector = pf::getText;
            }
            case NUMERO, DECIMAL -> {
                TextField tf = new TextField(vs);
                tf.getStyleClass().add("login-field");
                en.caja.getChildren().add(tf);
                en.lector = () -> {
                    String t = tf.getText().trim();
                    if (t.isEmpty()) return null;
                    try {
                        return c.tipo() == Tipo.NUMERO ? (Object) Integer.valueOf(t) : new BigDecimal(t);
                    } catch (NumberFormatException x) {
                        throw new IllegalArgumentException("Escribe un número válido.");
                    }
                };
            }
            case FECHA_HORA -> {
                DatePicker dp = new DatePicker();
                dp.getStyleClass().add("login-field");
                dp.setMaxWidth(Double.MAX_VALUE);
                TextField hora = new TextField();
                hora.getStyleClass().add("login-field");
                hora.setPromptText("HH:mm");
                hora.setPrefWidth(110);
                LocalDateTime ini = AdminEntidades.aLocal(valor);
                if (ini != null) {
                    dp.setValue(ini.toLocalDate());
                    hora.setText(ini.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                }
                HBox h = new HBox(10, dp, hora);
                HBox.setHgrow(dp, Priority.ALWAYS);
                en.caja.getChildren().add(h);
                en.lector = () -> {
                    if (dp.getValue() == null) return null;
                    LocalTime t;
                    try {
                        t = hora.getText().isBlank() ? LocalTime.MIDNIGHT : LocalTime.parse(hora.getText().trim());
                    } catch (DateTimeParseException x) {
                        throw new IllegalArgumentException("Hora inválida. Usa el formato HH:mm (por ejemplo 20:30).");
                    }
                    return dp.getValue().atTime(t).atZone(ZoneId.systemDefault()).toInstant().toString();
                };
            }
            case SELECT -> {
                ComboBox<Opcion> cb = new ComboBox<>();
                cb.getStyleClass().add("login-field");
                cb.setMaxWidth(Double.MAX_VALUE);
                cb.setPromptText("Selecciona…");
                List<Opcion> ops = c.from() != null ? AdminEntidades.opcionesDe(c.from()) : c.opciones();
                cb.getItems().setAll(ops);
                ops.stream().filter(o -> Objects.toString(o.valor()).equals(vs)).findFirst()
                        .ifPresent(o -> cb.getSelectionModel().select(o));
                en.caja.getChildren().add(cb);
                en.lector = () -> cb.getValue() == null ? null : cb.getValue().valor();
            }
            case IMAGEN -> {
                ImageView pv = new ImageView();
                pv.setFitWidth(96);
                pv.setFitHeight(96);
                pv.setPreserveRatio(true);
                if (!vs.isEmpty()) {
                    try { pv.setImage(new Image(vs, 96, 96, true, true, true)); } catch (IllegalArgumentException ignorado) { }
                }
                Label nombre = new Label(vs.isEmpty() ? "Sin imagen seleccionada" : "Imagen actual");
                nombre.getStyleClass().add("muted");
                Button elegir = new Button("Seleccionar imagen…");
                elegir.getStyleClass().add("tbl-btn");
                elegir.setOnAction(ev -> {
                    FileChooser fc = new FileChooser();
                    fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.jpg", "*.jpeg", "*.png", "*.webp", "*.gif"));
                    File f = fc.showOpenDialog(root.getScene().getWindow());
                    if (f != null) {
                        en.archivo = f;
                        nombre.setText(f.getName());
                        pv.setImage(new Image(f.toURI().toString(), 96, 96, true, true));
                        en.marcar(null);
                    }
                });
                HBox h = new HBox(14, pv, new VBox(8, elegir, nombre));
                h.setAlignment(Pos.CENTER_LEFT);
                en.caja.getChildren().add(h);
                en.lector = () -> vs.isEmpty() ? null : vs;
            }
            default -> {
                TextField tf = new TextField(vs);
                tf.getStyleClass().add("login-field");
                en.caja.getChildren().add(tf);
                en.lector = () -> tf.getText().trim();
            }
        }
        en.caja.getChildren().add(en.error);
        return en;
    }

    private void abrirFormulario(Map<String, Object> fila) {
        Entidad e = actual;
        boolean edicion = fila != null;
        Label titulo = new Label(edicion ? "Editar · " + e.titulo() : e.nuevo());
        titulo.getStyleClass().add("login-title");
        titulo.setWrapText(true);

        VBox campos = new VBox(14);
        List<Entrada> entradas = new ArrayList<>();
        for (Campo c : e.campos()) {
            if (c.solo() != 0 && c.solo() != (edicion ? 'e' : 'c')) continue;
            Entrada en = crearEntrada(c, edicion ? fila.get(c.key()) : null);
            entradas.add(en);
            campos.getChildren().add(en.caja);
        }
        ScrollPane sp = new ScrollPane(campos);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setMaxHeight(430);
        sp.getStyleClass().add("form-scroll");

        Label err = new Label();
        err.getStyleClass().add("err-label");
        err.setWrapText(true);
        mostrar(err, null);

        Button cancelar = new Button("Cancelar");
        cancelar.getStyleClass().add("tbl-btn");
        cancelar.setOnAction(ev -> cerrarModal());
        Button guardar = new Button("Guardar");
        guardar.getStyleClass().add("primary-btn");
        cancelar.setMaxWidth(Double.MAX_VALUE);
        guardar.setMaxWidth(Double.MAX_VALUE);
        HBox botones = new HBox(12, cancelar, guardar);
        HBox.setHgrow(cancelar, Priority.ALWAYS);
        HBox.setHgrow(guardar, Priority.ALWAYS);

        VBox card = new VBox(16, titulo, sp, err, botones);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(500);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        guardar.setOnAction(ev -> guardar(e, fila, entradas, err, guardar, cancelar));
        abrirModal(card);
    }

    private void guardar(Entidad e, Map<String, Object> fila, List<Entrada> entradas, Label err, Button btnG, Button btnC) {
        boolean ok = true;
        Map<String, Object> body = new LinkedHashMap<>(e.extra());
        File imagen = null;
        String claveImagen = null;
        mostrar(err, null);

        for (Entrada en : entradas) {
            en.marcar(null);
            Campo c = en.campo;
            Object v;
            try {
                v = en.lector.get();
            } catch (IllegalArgumentException x) {
                en.marcar(x.getMessage());
                ok = false;
                continue;
            }
            boolean vacio = v == null || (v instanceof String s && s.isBlank());
            if (c.tipo() == Tipo.IMAGEN) {
                if (en.archivo != null) { imagen = en.archivo; claveImagen = c.key(); }
                else if (!vacio) body.put(c.key(), v);
                continue;
            }
            if (vacio) {
                if (c.req()) { en.marcar("Este campo es obligatorio."); ok = false; }
                else if (c.tipo() != Tipo.PASSWORD) body.put(c.key(), null);
                continue;
            }
            if (c.tipo() == Tipo.PASSWORD && v.toString().length() < 6) {
                en.marcar("La contraseña debe tener al menos 6 caracteres.");
                ok = false;
                continue;
            }
            if (c.tipo() == Tipo.CORREO && !v.toString().matches("^\\S+@\\S+\\.\\S+$")) {
                en.marcar("Escribe un correo válido.");
                ok = false;
                continue;
            }
            body.put(c.key(), v);
        }
        if (!ok) {
            mostrar(err, "Revisa los campos marcados.");
            return;
        }

        final File imagenF = imagen;
        final String claveF = claveImagen;
        cargando(btnG, btnC, true, "Guardando…");
        CompletableFuture<Map<String, Object>> prep = imagenF == null
                ? CompletableFuture.completedFuture(body)
                : storage.subir(imagenF).thenApply(url -> { body.put(claveF, url); return body; });

        prep.thenCompose(b -> {
            if (fila == null) {
                if (e.alCrear() != null) e.alCrear().accept(b);
                return crud.crear(e.endpoint(), b);
            }
            Map<String, Object> m = new LinkedHashMap<>(fila);
            m.remove("password");
            m.putAll(b);
            return crud.actualizar(e.endpoint(), fila.get(e.idKey()), m);
        }).whenComplete((r, ex) -> Platform.runLater(() -> {
            cargando(btnG, btnC, false, "Guardar");
            if (ex != null) { mostrar(err, mensaje(ex)); return; }
            cerrarModal();
            toast(fila == null ? "Registro creado." : "Cambios guardados.");
            cargar();
        }));
    }

    private static void cargando(Button principal, Button secundario, boolean on, String texto) {
        principal.setDisable(on);
        secundario.setDisable(on);
        principal.setText(texto);
    }

    // ---------- eliminar (DELETE) y aprobar (PUT) ----------
    private void confirmarEliminar(Map<String, Object> fila) {
        Entidad e = actual;
        Label titulo = new Label("¿Eliminar registro?");
        titulo.getStyleClass().add("login-title");
        Label msg = new Label("Esta acción no se puede deshacer.");
        msg.getStyleClass().add("login-sub");
        Label err = new Label();
        err.getStyleClass().add("err-label");
        err.setWrapText(true);
        mostrar(err, null);

        Button cancelar = new Button("Cancelar");
        cancelar.getStyleClass().add("tbl-btn");
        cancelar.setOnAction(ev -> cerrarModal());
        Button eliminar = new Button("Eliminar");
        eliminar.getStyleClass().add("primary-btn");
        cancelar.setMaxWidth(Double.MAX_VALUE);
        eliminar.setMaxWidth(Double.MAX_VALUE);
        HBox botones = new HBox(12, cancelar, eliminar);
        HBox.setHgrow(cancelar, Priority.ALWAYS);
        HBox.setHgrow(eliminar, Priority.ALWAYS);

        eliminar.setOnAction(ev -> {
            cargando(eliminar, cancelar, true, "Eliminando…");
            crud.eliminar(e.endpoint(), fila.get(e.idKey())).whenComplete((r, ex) -> Platform.runLater(() -> {
                cargando(eliminar, cancelar, false, "Eliminar");
                if (ex != null) { mostrar(err, mensaje(ex)); return; }
                cerrarModal();
                toast("Registro eliminado.");
                cargar();
            }));
        });

        VBox card = new VBox(14, titulo, msg, err, botones);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(420);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        abrirModal(card);
    }

    private void aprobar(Map<String, Object> fila) {
        Entidad e = actual;
        Map<String, Object> m = new LinkedHashMap<>(fila);
        m.remove("password");
        m.putAll(e.accion().parche());
        crud.actualizar(e.endpoint(), fila.get(e.idKey()), m).whenComplete((r, ex) -> Platform.runLater(() -> {
            if (ex != null) { toast(mensaje(ex)); return; }
            toast("Cuenta aprobada.");
            cargar();
        }));
    }
}
