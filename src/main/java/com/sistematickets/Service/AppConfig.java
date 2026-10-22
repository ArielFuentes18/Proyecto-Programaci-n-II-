package com.sistematickets.Service;

/** Configuración central. Ajusta estos valores a tu proyecto. */
public final class AppConfig {
    private AppConfig() {}

    // Supabase Storage (Project Settings -> API)
    public static final String SUPA_URL = "https://TU-PROYECTO.supabase.co";
    public static final String SUPA_KEY = "TU_ANON_KEY";
    public static final String BUCKET = "artistas";

    // id_rol en la tabla roles
    public static final int ROL_ADMIN = 1;
    public static final int ROL_VENDEDOR = 2;

    // Rutas del backend
    public static final String EP_USUARIOS = "/api/usuarios";
    public static final String EP_ARTISTAS = "/api/artistas";
    public static final String EP_CONCIERTOS = "/api/conciertos";
    public static final String EP_INVENTARIO = "/api/inventario";
    public static final String EP_LOCALIDADES = "/api/localidades";
}
