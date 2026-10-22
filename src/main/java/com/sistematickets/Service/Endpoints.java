package com.sistematickets.Service;

public final class Endpoints {
    private Endpoints() {}
    public static final String LOGIN = "/api/auth/login";
    public static final String CONCIERTOS = "/api/conciertos";
    public static final String INVENTARIO_POR_CONCIERTO = "/api/inventario/concierto/"; // + id
    public static final String VENTAS = "/api/ventas";
}
