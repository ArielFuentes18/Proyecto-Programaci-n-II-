/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.sistematickets.Controller;
import java.text.Normalizer;
import java.util.*;
/**
 *
 * @author DIEGO JUAREZ
 */
public class ValidadorPais {
    private static final Map<String, String> INDICE = new HashMap<>();
    static {
        Locale es = Locale.of("es");
        for (String codigo : Locale.getISOCountries()) {
            Locale pais = Locale.of("", codigo);
            INDICE.put(clave(pais.getDisplayCountry(es)), pais.getDisplayCountry(es));     
            INDICE.putIfAbsent(clave(pais.getDisplayCountry(Locale.ENGLISH)),
                               pais.getDisplayCountry(es));                               
        }
    }
    private static String clave(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
    public static boolean esValido(String t)  { return t != null && INDICE.containsKey(clave(t)); }
    public static String normalizar(String t) { return t == null ? null : INDICE.get(clave(t)); }
}
