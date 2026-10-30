package com.sistematickets.Controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public final class QrSimulado {
    private QrSimulado() {}

    public static final int N = 29;

    public static boolean[][] matriz(String texto) {
        boolean[][] m = new boolean[N][N];
        boolean[][] reservado = new boolean[N][N];
        marca(m, reservado, 0, 0);
        marca(m, reservado, N - 7, 0);
        marca(m, reservado, 0, N - 7);
        byte[] h = hash(texto);
        int bit = 0;
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                if (reservado[y][x]) continue;
                int idx = bit++;
                int b = h[(idx / 8) % h.length] & 0xFF;
                
                m[y][x] = (((b >> (idx % 8)) & 1) ^ ((x * 7 + y * 13 + idx / 256) & 1)) == 1;
            }
        }
        return m;
    }

    private static void marca(boolean[][] m, boolean[][] r, int x0, int y0) {
        for (int y = -1; y <= 7; y++) {
            for (int x = -1; x <= 7; x++) {
                int xx = x0 + x, yy = y0 + y;
                if (xx < 0 || yy < 0 || xx >= N || yy >= N) continue;
                r[yy][xx] = true;
                boolean borde = x == 0 || x == 6 || y == 0 || y == 6;
                boolean centro = x >= 2 && x <= 4 && y >= 2 && y <= 4;
                m[yy][xx] = x >= 0 && x <= 6 && y >= 0 && y <= 6 && (borde || centro);
            }
        }
    }

    private static byte[] hash(String t) {
        try {
            return MessageDigest.getInstance("SHA-256").digest((t == null ? "" : t).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            return new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        }
    }
}
