package com.sistematickets.Service;

public final class Endpoints {
    private Endpoints() {}
    public static final String LOGIN = "/api/usuarios/login";
    public static final String CONCIERTOS = "/api/conciertos";
    public static final String INVENTARIO_POR_CONCIERTO = "/api/inventario_boletos/concierto/"; // + id
    public static final String INVENTARIO = "/api/inventario_boletos";
    public static final String REGISTRO = "/api/usuarios/registro";
    public static final String VENTAS = "/api/ventas/create";
}
