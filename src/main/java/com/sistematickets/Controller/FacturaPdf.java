package com.sistematickets.Controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;


public final class FacturaPdf {
    private FacturaPdf() {}

    private static final double W = 595.28, H = 841.89, M = 44;
    private static final Charset WIN = Charset.forName("windows-1252");

 
    private static final double[] MORADO_OSCURO = rgb("#2e1065");
    private static final double[] MORADO = rgb("#6d28d9");
    private static final double[] LILA = rgb("#a78bfa");
    private static final double[] LILA_CLARO = rgb("#ede9fe");
    private static final double[] TEXTO = rgb("#1f1b2e");
    private static final double[] TEXTO_SUAVE = rgb("#3b3556");
    private static final double[] GRIS = rgb("#8a84a3");
    private static final double[] LINEA = rgb("#ece8f7");
    private static final double[] BLANCO = {1, 1, 1};

    public static void guardar(FacturaDatos d, File destino) throws IOException {
        Files.write(destino.toPath(), generar(d));
    }

    public static byte[] generar(FacturaDatos d) throws IOException {
        Lienzo c = new Lienzo();

   
        c.rect(0, H - 118, W, 118, MORADO_OSCURO);
        c.rect(0, H - 122, W, 4, LILA);
        c.texto(M, H - 46, "FACTURA", false, 10, rgb("#ddd6fe"));
        c.texto(M, H - 78, d.numero(), true, 24, BLANCO);
        double pillW = 62;
        c.redondo(W - M - pillW, H - 56, pillW, 20, 10, LILA_CLARO);
        c.texto(W - M - pillW + (pillW - ancho("PAGADO", true, 9)) / 2, H - 49.5, "PAGADO", true, 9, rgb("#5b21b6"));
        c.textoDerecha(W - M, H - 82, d.fechaTexto(), false, 10, BLANCO);

       
        double y = H - 162;
        double colDer = W / 2 + 14;
        double anchoCol = W / 2 - M - 14;

        double yIzq = y;
        c.texto(M, yIzq, "CLIENTE", true, 8, GRIS);
        yIzq -= 16;
        for (String l : envolver(d.clienteNombre(), anchoCol, true, 12)) { c.texto(M, yIzq, l, true, 12, MORADO_OSCURO); yIzq -= 15; }
        if (d.clienteDoc() != null && !d.clienteDoc().isBlank()) { c.texto(M, yIzq, d.clienteDoc(), false, 10, TEXTO_SUAVE); yIzq -= 14; }
        if (d.atendidoPor() != null && !d.atendidoPor().isBlank()) {
            yIzq -= 4;
            c.texto(M, yIzq, "ATENDIDO POR", true, 8, GRIS);
            yIzq -= 14;
            c.texto(M, yIzq, d.atendidoPor(), false, 10, TEXTO_SUAVE);
            yIzq -= 14;
        }

        double yDer = y;
        c.texto(colDer, yDer, "EVENTO", true, 8, GRIS);
        yDer -= 16;
        for (String l : envolver(d.evento(), anchoCol, true, 12)) { c.texto(colDer, yDer, l, true, 12, MORADO_OSCURO); yDer -= 15; }
        for (String s : new String[]{d.artista(), d.fechaEvento(), d.recinto()}) {
            if (s == null || s.isBlank()) continue;
            for (String l : envolver(s, anchoCol, false, 10)) { c.texto(colDer, yDer, l, false, 10, TEXTO_SUAVE); yDer -= 14; }
        }

       
        y = Math.min(yIzq, yDer) - 22;
        double xCant = W - M - 190, xPrecio = W - M - 100, xSub = W - M;
        c.texto(M, y, "LOCALIDAD", true, 8, GRIS);
        c.textoDerecha(xCant, y, "CANT.", true, 8, GRIS);
        c.textoDerecha(xPrecio, y, "PRECIO", true, 8, GRIS);
        c.textoDerecha(xSub, y, "SUBTOTAL", true, 8, GRIS);
        y -= 8;
        c.linea(M, y, W - M, y, 0.8, LINEA);
        y -= 18;
        for (LineaCompra l : d.lineas()) {
            c.texto(M, y, recortar(l.localidad(), xCant - M - 50, false, 11), false, 11, TEXTO);
            c.textoDerecha(xCant, y, String.valueOf(l.cantidad()), false, 11, TEXTO);
            c.textoDerecha(xPrecio, y, Impuestos.q(l.precio()), false, 11, TEXTO);
            c.textoDerecha(xSub, y, Impuestos.q(l.subtotal()), true, 11, TEXTO);
            y -= 8;
            c.linea(M, y, W - M, y, 0.5, LINEA);
            y -= 16;
        }

      
        y -= 4;
        c.texto(xPrecio - 90, y, "Subtotal (sin IVA)", false, 10, TEXTO_SUAVE);
        c.textoDerecha(xSub, y, Impuestos.q(d.base()), false, 10, TEXTO);
        y -= 16;
        c.texto(xPrecio - 90, y, "IVA 12 %", false, 10, TEXTO_SUAVE);
        c.textoDerecha(xSub, y, Impuestos.q(d.iva()), false, 10, TEXTO);
        y -= 12;
        c.linea(xPrecio - 90, y, W - M, y, 0.8, LINEA);
        y -= 22;
        c.texto(xPrecio - 90, y, "TOTAL PAGADO", true, 10, MORADO_OSCURO);
        c.textoDerecha(xSub, y, Impuestos.q(d.total()), true, 18, MORADO_OSCURO);

       
        y -= 34;
        c.linea(M, y, W - M, y, 0.8, LINEA);
        y -= 22;
        double qr = 124;
        double xQr = W - M - qr;
        double yTopPie = y;

        double yp = yTopPie;
        double anchoPie = xQr - M - 24;
        c.texto(M, yp, "MÉTODO DE PAGO", true, 8, GRIS);
        yp -= 15;
        c.texto(M, yp, d.metodo(), true, 12, MORADO_OSCURO);
        yp -= 15;
        for (String l : envolver(d.detallePago(), anchoPie, false, 10)) { c.texto(M, yp, l, false, 10, TEXTO_SUAVE); yp -= 14; }
        yp -= 10;
        c.texto(M, yp, "CÓDIGO DE ACCESO", true, 8, GRIS);
        yp -= 15;
        c.texto(M, yp, d.codigo(), true, 12, MORADO_OSCURO);
        yp -= 16;
        c.texto(M, yp, "Cada ticket trae su propio QR (te llegan por correo).", false, 9, GRIS);

      
        boolean[][] m = com.sistematickets.Service.QrCodigo.modulos(d.codigo());
        int n = m.length;
        double mod = qr / (n + 2);
        double yQr = yTopPie - qr + 8;
        c.rect(xQr, yQr, qr, qr, BLANCO);
        c.borde(xQr, yQr, qr, qr, 0.8, LINEA);
        for (int fy = 0; fy < n; fy++) {
            for (int fx = 0; fx < n; fx++) {
                if (m[fy][fx]) c.rect(xQr + (fx + 1) * mod, yQr + qr - (fy + 2) * mod, mod + 0.15, mod + 0.15, TEXTO);
            }
        }
        c.textoCentro(xQr + qr / 2, yQr - 12, "QR de la orden", false, 8, GRIS);

        // ---------- pie ----------
        c.linea(M, 58, W - M, 58, 0.5, LINEA);

        return c.construir("Factura " + d.numero());
    }


    private static final class Lienzo {
        private final ByteArrayOutputStream cs = new ByteArrayOutputStream();

        private void op(String s) {
            cs.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
            cs.write('\n');
        }

        private static String n(double v) { return String.format(Locale.US, "%.2f", v); }

        private static String color(double[] c) { return n3(c[0]) + " " + n3(c[1]) + " " + n3(c[2]); }

        private static String n3(double v) { return String.format(Locale.US, "%.3f", v); }

        void rect(double x, double y, double w, double h, double[] c) {
            op(color(c) + " rg " + n(x) + " " + n(y) + " " + n(w) + " " + n(h) + " re f");
        }

        void borde(double x, double y, double w, double h, double grosor, double[] c) {
            op(color(c) + " RG " + n(grosor) + " w " + n(x) + " " + n(y) + " " + n(w) + " " + n(h) + " re S");
        }

        void linea(double x1, double y1, double x2, double y2, double grosor, double[] c) {
            op(color(c) + " RG " + n(grosor) + " w " + n(x1) + " " + n(y1) + " m " + n(x2) + " " + n(y2) + " l S");
        }

        void redondo(double x, double y, double w, double h, double r, double[] c) {
            double k = 0.5523 * r;
            op(color(c) + " rg");
            op(n(x + r) + " " + n(y) + " m");
            op(n(x + w - r) + " " + n(y) + " l");
            op(n(x + w - r + k) + " " + n(y) + " " + n(x + w) + " " + n(y + r - k) + " " + n(x + w) + " " + n(y + r) + " c");
            op(n(x + w) + " " + n(y + h - r) + " l");
            op(n(x + w) + " " + n(y + h - r + k) + " " + n(x + w - r + k) + " " + n(y + h) + " " + n(x + w - r) + " " + n(y + h) + " c");
            op(n(x + r) + " " + n(y + h) + " l");
            op(n(x + r - k) + " " + n(y + h) + " " + n(x) + " " + n(y + h - r + k) + " " + n(x) + " " + n(y + h - r) + " c");
            op(n(x) + " " + n(y + r) + " l");
            op(n(x) + " " + n(y + r - k) + " " + n(x + r - k) + " " + n(y) + " " + n(x + r) + " " + n(y) + " c");
            op("f");
        }

        void texto(double x, double y, String t, boolean negrita, double tam, double[] c) {
            if (t == null || t.isEmpty()) return;
            op("BT /" + (negrita ? "F2" : "F1") + " " + n(tam) + " Tf " + color(c) + " rg " + n(x) + " " + n(y) + " Td <"
                    + hex(t) + "> Tj ET");
        }

        void textoDerecha(double xDerecha, double y, String t, boolean negrita, double tam, double[] c) {
            texto(xDerecha - ancho(t, negrita, tam), y, t, negrita, tam, c);
        }

        void textoCentro(double xCentro, double y, String t, boolean negrita, double tam, double[] c) {
            texto(xCentro - ancho(t, negrita, tam) / 2, y, t, negrita, tam, c);
        }

        byte[] construir(String titulo) throws IOException {
            byte[] contenido = comprimir(cs.toByteArray());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            List<Integer> offsets = new ArrayList<>();
            out.writeBytes("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n".getBytes(StandardCharsets.ISO_8859_1));

            objeto(out, offsets, "<< /Type /Catalog /Pages 2 0 R >>");
            objeto(out, offsets, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
            objeto(out, offsets, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + n(W) + " " + n(H) + "] "
                    + "/Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> /Contents 4 0 R >>");
            offsets.add(out.size());
            out.writeBytes(("4 0 obj\n<< /Length " + contenido.length + " /Filter /FlateDecode >>\nstream\n")
                    .getBytes(StandardCharsets.US_ASCII));
            out.writeBytes(contenido);
            out.writeBytes("\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII));
            objeto(out, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
            objeto(out, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
            objeto(out, offsets, "<< /Title <" + hexUtf16(titulo) + "> /Creator (Sistema de Venta de Tickets) /Producer (Sistema de Venta de Tickets) >>");

            int xref = out.size();
            StringBuilder sb = new StringBuilder("xref\n0 " + (offsets.size() + 1) + "\n0000000000 65535 f \n");
            for (int off : offsets) sb.append(String.format(Locale.US, "%010d 00000 n \n", off));
            sb.append("trailer\n<< /Size ").append(offsets.size() + 1).append(" /Root 1 0 R /Info 7 0 R >>\nstartxref\n")
                    .append(xref).append("\n%%EOF\n");
            out.writeBytes(sb.toString().getBytes(StandardCharsets.US_ASCII));
            return out.toByteArray();
        }

        private static void objeto(ByteArrayOutputStream out, List<Integer> offsets, String cuerpo) {
            offsets.add(out.size());
            int num = offsets.size();
            out.writeBytes((num + " 0 obj\n" + cuerpo + "\nendobj\n").getBytes(StandardCharsets.US_ASCII));
        }

        private static byte[] comprimir(byte[] datos) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (DeflaterOutputStream dos = new DeflaterOutputStream(bos, new Deflater(Deflater.BEST_COMPRESSION))) {
                dos.write(datos);
            }
            return bos.toByteArray();
        }
    }

 
    private static String hex(String t) {
        StringBuilder sb = new StringBuilder();
        for (byte b : limpiar(t).getBytes(WIN)) sb.append(String.format("%02X", b & 0xFF));
        return sb.toString();
    }

    private static String hexUtf16(String t) {
        StringBuilder sb = new StringBuilder("FEFF");
        for (char ch : limpiar(t).toCharArray()) sb.append(String.format("%04X", (int) ch));
        return sb.toString();
    }

    private static String limpiar(String t) {
        if (t == null) return "";
        StringBuilder sb = new StringBuilder();
        var enc = WIN.newEncoder();
        for (char ch : t.toCharArray()) sb.append(enc.canEncode(ch) ? ch : '?');
        return sb.toString();
    }

    private static final int[] ANCHO_REG = {
            278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
            556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556,
            1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
            667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556,
            333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556,
            556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584};
    private static final int[] ANCHO_BOLD = {
            278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278,
            556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 333, 333, 584, 584, 584, 611,
            975, 722, 722, 722, 722, 667, 611, 778, 722, 278, 556, 722, 611, 833, 722, 778,
            667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 333, 278, 333, 584, 556,
            333, 556, 611, 556, 611, 556, 333, 611, 611, 278, 278, 556, 278, 889, 611, 611,
            611, 611, 389, 556, 333, 611, 556, 778, 556, 556, 500, 389, 280, 389, 584};

    static double ancho(String t, boolean negrita, double tam) {
        int[] tabla = negrita ? ANCHO_BOLD : ANCHO_REG;
        double suma = 0;
        for (char ch : limpiar(t).toCharArray()) suma += anchoCaracter(ch, tabla, negrita);
        return suma * tam / 1000.0;
    }

    private static int anchoCaracter(char ch, int[] tabla, boolean negrita) {
        if (ch >= 32 && ch <= 126) return tabla[ch - 32];
        switch (ch) {
            case '·': return 278;
            case '¿': return negrita ? 611 : 611;
            case '¡': return 333;
            case '×': return 584;
            case '•': return 350;
            case '–': return 556;
            case '—': return 1000;
            default: break;
        }
        String base = Normalizer.normalize(String.valueOf(ch), Normalizer.Form.NFD);
        char b = base.charAt(0);
        if (b >= 32 && b <= 126) return tabla[b - 32];
        return 556;
    }
    private static List<String> envolver(String texto, double max, boolean negrita, double tam) {
        List<String> lineas = new ArrayList<>();
        if (texto == null || texto.isBlank()) return lineas;
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.trim().split("\\s+")) {
            String prueba = actual.length() == 0 ? palabra : actual + " " + palabra;
            if (ancho(prueba, negrita, tam) <= max || actual.length() == 0) {
                actual = new StringBuilder(prueba);
            } else {
                lineas.add(actual.toString());
                actual = new StringBuilder(palabra);
            }
        }
        if (actual.length() > 0) lineas.add(actual.toString());
        return lineas;
    }

    private static String recortar(String texto, double max, boolean negrita, double tam) {
        if (texto == null) return "";
        if (ancho(texto, negrita, tam) <= max) return texto;
        String t = texto;
        while (t.length() > 1 && ancho(t + "…", negrita, tam) > max) t = t.substring(0, t.length() - 1);
        return t + "…";
    }

    private static double[] rgb(String hex) {
        int v = Integer.parseInt(hex.substring(1), 16);
        return new double[]{((v >> 16) & 255) / 255.0, ((v >> 8) & 255) / 255.0, (v & 255) / 255.0};
    }
}