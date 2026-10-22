package com.sistematickets;

/**
 * Punto de entrada alterno para ejecutar desde NetBeans / classpath.
 * No extiende Application, así el lanzador de Java no exige módulos de JavaFX al arrancar.
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
