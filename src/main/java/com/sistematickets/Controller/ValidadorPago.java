package com.sistematickets.Controller;

import java.time.YearMonth;

public final class ValidadorPago {
    private ValidadorPago() {}

    public static String soloDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    public static boolean luhn(String numero) {
        String d = soloDigitos(numero);
        if (d.length() < 13 || d.length() > 19) return false;
        int suma = 0;
        boolean doble = false;
        for (int i = d.length() - 1; i >= 0; i--) {
            int n = d.charAt(i) - '0';
            if (doble) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            suma += n;
            doble = !doble;
        }
        return suma % 10 == 0;
    }


    public static boolean numeroValido(String numero) {
        int n = soloDigitos(numero).length();
        return n >= 13 && n <= 16;
    }

    public static String marca(String numero) {
        String d = soloDigitos(numero);
        if (d.startsWith("4")) return "Visa";
        if (d.length() >= 2) {
            int dos = Integer.parseInt(d.substring(0, 2));
            if (dos >= 51 && dos <= 55) return "Mastercard";
            if (dos == 34 || dos == 37) return "American Express";
        }
        return "Tarjeta";
    }

    
    public static boolean vencimientoValido(String mmaa, YearMonth hoy) {
        if (mmaa == null || !mmaa.matches("\\d{2}/\\d{2}")) return false;
        int mes = Integer.parseInt(mmaa.substring(0, 2));
        int anio = 2000 + Integer.parseInt(mmaa.substring(3, 5));
        if (mes < 1 || mes > 12) return false;
        YearMonth v = YearMonth.of(anio, mes);
        return !v.isBefore(hoy) && v.isBefore(hoy.plusYears(21));
    }

    public static boolean cvvValido(String cvv) {
        String d = soloDigitos(cvv);
        return d.length() == 3 || d.length() == 4;
    }

 
    public static String agruparNumero(String s) {
        String d = soloDigitos(s);
        if (d.length() > 16) d = d.substring(0, 16);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < d.length(); i++) {
            if (i > 0 && i % 4 == 0) sb.append(' ');
            sb.append(d.charAt(i));
        }
        return sb.toString();
    }


    public static String formatearVencimiento(String s) {
        String d = soloDigitos(s);
        if (d.length() > 4) d = d.substring(0, 4);
        return d.length() <= 2 ? d : d.substring(0, 2) + "/" + d.substring(2);
    }
}
