package com.sistematickets.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class CorreoConfig {
    private final String host, usuario, clave, remitente, nombre;
    private final int puerto;

    private CorreoConfig(String host, int puerto, String usuario, String clave, String remitente, String nombre) {
        this.host = host;
        this.puerto = puerto;
        this.usuario = usuario;
        this.clave = clave;
        this.remitente = remitente;
        this.nombre = nombre;
    }

    public static CorreoConfig cargar() {
        Properties p = new Properties();
        try (InputStream in = CorreoConfig.class.getResourceAsStream("/conexion/correo.properties")) {
            if (in != null) p.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException ignorado) {
       
        }
        String host = valor(p, "CORREO_HOST", "host", "smtp.gmail.com");
        int puerto = 587;
        try {
            puerto = Integer.parseInt(valor(p, "CORREO_PORT", "port", "587"));
        } catch (NumberFormatException ignorado) { }
        String usuario = valor(p, "CORREO_USER", "usuario", "");
        String clave = valor(p, "CORREO_PASS", "clave", "");
        String from = valor(p, "CORREO_FROM", "remitente", usuario);
        String nombre = valor(p, "CORREO_NOMBRE", "nombre", "Sistema de Venta de Tickets");
        return new CorreoConfig(host, puerto, usuario, clave, from, nombre);
    }

    private static String valor(Properties p, String env, String clave, String defecto) {
        String e = System.getenv(env);
        if (e != null && !e.isBlank()) return e.trim();
        String v = p.getProperty(clave);
        return v == null || v.isBlank() ? defecto : v.trim();
    }


    public boolean configurado() { return !usuario.isBlank() && !clave.isBlank(); }

    public String host() { return host; }
    public int puerto() { return puerto; }
    public String usuario() { return usuario; }
    public String clave() { return clave; }
    public String remitente() { return remitente.isBlank() ? usuario : remitente; }
    public String nombre() { return nombre; }
}
