package com.sistematickets.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public final class QrCodigo {
    private QrCodigo() {}

    private static final int MARGEN_PNG = 4;     

    public static boolean[][] modulos(String texto) {
        try {
            BitMatrix bm = new QRCodeWriter().encode(texto == null ? "" : texto, BarcodeFormat.QR_CODE, 1, 1,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                           EncodeHintType.MARGIN, 0,
                           EncodeHintType.CHARACTER_SET, "UTF-8"));
            int n = bm.getWidth();
            boolean[][] m = new boolean[n][n];
            for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) m[y][x] = bm.get(x, y);
            return m;
        } catch (WriterException e) {
            throw new IllegalStateException("No se pudo generar el QR: " + e.getMessage(), e);
        }
    }

    
    public static byte[] png(String texto, int lado) throws IOException {
        boolean[][] m = modulos(texto);
        int n = m.length;
        int escala = Math.max(1, lado / (n + 2 * MARGEN_PNG));
        int tam = (n + 2 * MARGEN_PNG) * escala;
        BufferedImage img = new BufferedImage(tam, tam, BufferedImage.TYPE_INT_RGB);
        int blanco = 0xFFFFFF, oscuro = 0x1F1B2E;
        for (int py = 0; py < tam; py++) {
            for (int px = 0; px < tam; px++) {
                int mx = px / escala - MARGEN_PNG, my = py / escala - MARGEN_PNG;
                boolean negro = mx >= 0 && my >= 0 && mx < n && my < n && m[my][mx];
                img.setRGB(px, py, negro ? oscuro : blanco);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}
