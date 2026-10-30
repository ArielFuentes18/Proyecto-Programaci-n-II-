package com.sistematickets.Service;

import java.util.List;


public final class AppConfig {
    private AppConfig() {}


    public static final String EP_SUBIR_IMAGEN = "/api/artistas/imagen";


    public static final int ROL_ADMIN = 1;
    public static final int ROL_VENDEDOR = 2;


    public static final String EP_ESTADISTICAS = "/api/estadisticas";
    public static final String EP_USUARIOS = "/api/usuarios";
    public static final String EP_ARTISTAS = "/api/artistas";
    public static final String EP_CONCIERTOS = "/api/conciertos";
    public static final String EP_INVENTARIO = "/api/inventario_boletos";
  
    public static final String EP_LOCALIDADES = "/api/localidades";

    public static final List<String> LOCALIDADES = List.of("VIP", "Platea", "General");
}
