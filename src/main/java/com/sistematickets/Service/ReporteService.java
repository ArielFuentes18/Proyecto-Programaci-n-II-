package com.sistematickets.Service;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ReporteService {
    private ReporteService() {}

    public enum Tipo {
        BARRAS("grafico-barras"), LINEAS("grafico-lineas"), PASTEL("grafico-pastel");
        final String plantilla;
        Tipo(String plantilla) { this.plantilla = plantilla; }
    }

    public record Dato(String etiqueta, double valor) {}

    private static final Map<String, JasperReport> CACHE = new ConcurrentHashMap<>();

    static {
        DefaultJasperReportsContext.getInstance().setProperty("net.sf.jasperreports.awt.ignore.missing.font", "true");
    }

    private static JasperReport compilar(String nombre) throws JRException {
        JasperReport cacheado = CACHE.get(nombre);
        if (cacheado != null) return cacheado;
        try (InputStream in = ReporteService.class.getResourceAsStream("/com/sistematickets/reportes/" + nombre + ".jrxml")) {
            if (in == null) throw new JRException("No se encontró la plantilla " + nombre + ".jrxml");
            JasperReport r = JasperCompileManager.compileReport(in);
            CACHE.put(nombre, r);
            return r;
        } catch (IOException e) {
            throw new JRException(e);
        }
    }

    public static JasperPrint grafico(Tipo tipo, String titulo, String subtitulo, String nota, String pie,
                                      List<Dato> datos) throws JRException {
        List<Map<String, ?>> filas = new ArrayList<>();
        for (Dato d : datos) {
            Map<String, Object> m = new HashMap<>();
            m.put("etiqueta", d.etiqueta());
            m.put("valor", Double.valueOf(d.valor()));
            filas.add(m);
        }
        Map<String, Object> params = new HashMap<>();
        params.put("TITULO", titulo);
        params.put("SUBTITULO", subtitulo);
        params.put("NOTA", nota);
        params.put("PIE", pie);
        params.put("DATOS", new JRMapCollectionDataSource(filas));
        return JasperFillManager.fillReport(compilar(tipo.plantilla), params, new JREmptyDataSource());
    }

    public static JasperPrint resumen(Map<String, Object> parametros, List<Map<String, String>> tabla) throws JRException {
        List<Map<String, ?>> filas = new ArrayList<>(tabla);
        return JasperFillManager.fillReport(compilar("resumen-gerencial"), parametros, new JRMapCollectionDataSource(filas));
    }

    public static byte[] aPng(JasperPrint print, float zoom) throws JRException, IOException {
        Image img = JasperPrintManager.printPageToImage(print, 0, zoom);
        BufferedImage bi;
        if (img instanceof BufferedImage b) {
            bi = b;
        } else {
            bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bi.createGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(bi, "png", out);
        return out.toByteArray();
    }

    public static void exportarPdf(List<JasperPrint> paginas, File destino) throws JRException {
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(SimpleExporterInput.getInstance(paginas));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destino));
        exporter.exportReport();
    }
}
