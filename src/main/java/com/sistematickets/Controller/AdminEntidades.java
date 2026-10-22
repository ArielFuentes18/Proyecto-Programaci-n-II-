package com.sistematickets.Controller;

import com.sistematickets.Service.AppConfig;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** Describe cada sección del panel (columnas, campos, rutas). El panel es genérico y se construye desde aquí. */
public final class AdminEntidades {
    private AdminEntidades() {}

    public enum Tipo { TEXTO, CORREO, PASSWORD, NUMERO, DECIMAL, FECHA_HORA, SELECT, IMAGEN }

    public record Opcion(Object valor, String texto) {
        @Override public String toString() { return texto; }
    }
    public record Campo(String key, String label, Tipo tipo, boolean req, char solo, String from, List<Opcion> opciones) {}
    public record Columna(String titulo, Function<Map<String, Object>, String> valor, boolean imagen, boolean pill) {}
    public record Lookup(String endpoint, String idKey, String labelKey) {}
    public record Accion(String texto, Predicate<Map<String, Object>> visible, Map<String, Object> parche) {}
    public record Entidad(String clave, String titulo, String nuevo, String endpoint, String idKey,
                          List<String> lookups, Predicate<Map<String, Object>> filtro, Map<String, Object> extra,
                          List<Columna> columnas, List<Campo> campos, Accion accion,
                          Consumer<Map<String, Object>> alCrear) {}

    public static final Map<String, Lookup> LOOKUPS = Map.of(
            "cantantes", new Lookup(AppConfig.EP_ARTISTAS, "id_artista", "nombre_artistico"),
            "conciertos", new Lookup(AppConfig.EP_CONCIERTOS, "id_concierto", "titulo_evento"),
            "localidades", new Lookup(AppConfig.EP_LOCALIDADES, "id_localidad", "nombre"));

    /** Catálogos cargados (para selects y para mostrar nombres en lugar de ids). */
    public static final Map<String, List<Map<String, Object>>> CATALOGOS = new HashMap<>();

    // ---------- utilidades ----------
    public static String str(Object o) { return o == null ? "" : String.valueOf(o); }

    public static String etiqueta(String lookup, Object id) {
        Lookup l = LOOKUPS.get(lookup);
        for (Map<String, Object> r : CATALOGOS.getOrDefault(lookup, List.of())) {
            if (str(r.get(l.idKey())).equals(str(id))) return str(r.get(l.labelKey()));
        }
        return str(id);
    }

    public static List<Opcion> opcionesDe(String lookup) {
        Lookup l = LOOKUPS.get(lookup);
        List<Opcion> out = new ArrayList<>();
        for (Map<String, Object> r : CATALOGOS.getOrDefault(lookup, List.of())) {
            out.add(new Opcion(r.get(l.idKey()), str(r.get(l.labelKey()))));
        }
        return out;
    }

    public static LocalDateTime aLocal(Object o) {
        String s = str(o);
        if (s.isEmpty()) return null;
        try {
            return OffsetDateTime.parse(s).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(s);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private static String fecha(Object o) {
        LocalDateTime d = aLocal(o);
        return d == null ? str(o) : d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private static String dinero(Object o) {
        try {
            return "Q" + new BigDecimal(str(o)).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return str(o);
        }
    }

    private static boolean activo(Object o) {
        if (o instanceof Boolean b) return b;
        return "1".equals(str(o)) || "true".equalsIgnoreCase(str(o));
    }

    static String rolOf(Map<String, Object> u) {
        String r = str(u.get("rol"));
        if (r.isEmpty()) r = str(u.get("nombre_rol"));
        if (!r.isEmpty()) return r.toLowerCase();
        String id = str(u.get("id_rol"));
        if (id.equals(String.valueOf(AppConfig.ROL_ADMIN))) return "administrador";
        if (id.equals(String.valueOf(AppConfig.ROL_VENDEDOR))) return "vendedor";
        return "";
    }

    private static Campo campo(String k, String l, Tipo t, boolean req) { return new Campo(k, l, t, req, '\0', null, null); }
    private static Campo solo(char s, Campo c) { return new Campo(c.key(), c.label(), c.tipo(), c.req(), s, c.from(), c.opciones()); }
    private static Campo sel(String k, String l, String from) { return new Campo(k, l, Tipo.SELECT, true, '\0', from, null); }
    private static Campo sel(String k, String l, List<Opcion> ops) { return new Campo(k, l, Tipo.SELECT, true, '\0', null, ops); }
    private static Columna col(String t, Function<Map<String, Object>, String> f) { return new Columna(t, f, false, false); }
    private static Columna pill(String t, Function<Map<String, Object>, String> f) { return new Columna(t, f, false, true); }

    // ---------- secciones ----------
    private static Entidad usuarios(String clave, String titulo, String nuevo, String rol, int idRol, boolean admin) {
        return new Entidad(clave, titulo, nuevo, AppConfig.EP_USUARIOS, "id_usuario", List.of(),
                u -> rolOf(u).equals(rol), Map.of("id_rol", idRol),
                List.of(col("Nombre", r -> str(r.get("nombre"))),
                        col("Correo", r -> str(r.get("correo"))),
                        pill("Estado", r -> activo(r.get("status")) ? "Activo" : "Pendiente")),
                List.of(campo("nombre", "Nombre", Tipo.TEXTO, true),
                        campo("correo", "Correo electrónico", Tipo.CORREO, true),
                        solo('c', campo("password", "Contraseña (mínimo 6)", Tipo.PASSWORD, true)),
                        sel("status", "Estado", List.of(new Opcion(1, "Activo"), new Opcion(0, "Pendiente / inactivo")))),
                admin ? new Accion("Aprobar", r -> !activo(r.get("status")), Map.of("status", 1)) : null,
                null);
    }

    public static Map<String, Entidad> mapa() {
        Map<String, Entidad> m = new LinkedHashMap<>();
        m.put("vendedores", usuarios("vendedores", "Vendedores", "Nuevo vendedor", "vendedor", AppConfig.ROL_VENDEDOR, false));

        m.put("tickets", new Entidad("tickets", "Tickets", "Nuevo ticket", AppConfig.EP_INVENTARIO, "id_inventario",
                List.of("conciertos", "localidades"), r -> true, Map.of(),
                List.of(col("Concierto", r -> etiqueta("conciertos", r.get("id_concierto"))),
                        col("Localidad", r -> etiqueta("localidades", r.get("id_localidad"))),
                        col("Precio", r -> dinero(r.get("precio"))),
                        col("Total", r -> str(r.get("cantidad_total"))),
                        pill("Disponibles", r -> str(r.get("cantidad_disponible")))),
                List.of(sel("id_concierto", "Concierto", "conciertos"),
                        sel("id_localidad", "Localidad", "localidades"),
                        campo("precio", "Precio (Q)", Tipo.DECIMAL, true),
                        campo("cantidad_total", "Cantidad total", Tipo.NUMERO, true),
                        solo('e', campo("cantidad_disponible", "Cantidad disponible", Tipo.NUMERO, true))),
                null, b -> b.put("cantidad_disponible", b.get("cantidad_total"))));

        m.put("conciertos", new Entidad("conciertos", "Conciertos", "Nuevo concierto", AppConfig.EP_CONCIERTOS, "id_concierto",
                List.of("cantantes"), r -> true, Map.of(),
                List.of(col("Evento", r -> str(r.get("titulo_evento"))),
                        col("Cantante", r -> etiqueta("cantantes", r.get("id_artista"))),
                        col("Fecha", r -> fecha(r.get("fecha_concierto"))),
                        col("Recinto", r -> str(r.get("recinto"))),
                        pill("Estado", r -> str(r.get("estado")))),
                List.of(campo("titulo_evento", "Título del evento", Tipo.TEXTO, true),
                        sel("id_artista", "Cantante", "cantantes"),
                        campo("fecha_concierto", "Fecha y hora", Tipo.FECHA_HORA, true),
                        campo("recinto", "Recinto", Tipo.TEXTO, true),
                        sel("estado", "Estado", List.of(new Opcion("programado", "Programado"), new Opcion("activo", "Activo"),
                                new Opcion("finalizado", "Finalizado"), new Opcion("cancelado", "Cancelado")))),
                null, null));

        m.put("cantantes", new Entidad("cantantes", "Cantantes", "Nuevo cantante", AppConfig.EP_ARTISTAS, "id_artista",
                List.of(), r -> true, Map.of(),
                List.of(new Columna("Foto", r -> "", true, false),
                        col("Nombre artístico", r -> str(r.get("nombre_artistico"))),
                        col("Género", r -> str(r.get("genero_musical"))),
                        col("País", r -> str(r.get("pais_origen")))),
                List.of(campo("nombre_artistico", "Nombre artístico", Tipo.TEXTO, true),
                        campo("genero_musical", "Género musical", Tipo.TEXTO, true),
                        campo("pais_origen", "País de origen", Tipo.TEXTO, false),
                        campo("imagen_url", "Foto del cantante (bucket artistas)", Tipo.IMAGEN, false)),
                null, null));

        m.put("admins", usuarios("admins", "Administradores", "Nuevo administrador", "administrador", AppConfig.ROL_ADMIN, true));
        return m;
    }
}
