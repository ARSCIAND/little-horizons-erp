package com.littlehorizons.erp.service.invoice;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Builds a visual QR-like matrix (simulated, as allowed by the project statement).
 * <p>
 * The matrix has the real structure of a version 3 QR symbol (29 x 29): three finder patterns,
 * separators, timing patterns and an alignment pattern. The data area is filled deterministically
 * with the SHA-256 digest stream of the encoded content, so every invoice gets a unique and
 * reproducible pattern that "contains" its information. It is not meant to be scanned.
 */
public final class SimulatedQrGenerator {

    public static final int SIZE = 29;

    private SimulatedQrGenerator() {
    }

    public static boolean[][] generate(String content) {
        boolean[][] modules = new boolean[SIZE][SIZE];
        boolean[][] reserved = new boolean[SIZE][SIZE];

        drawFinder(modules, reserved, 0, 0);
        drawFinder(modules, reserved, 0, SIZE - 7);
        drawFinder(modules, reserved, SIZE - 7, 0);
        drawAlignment(modules, reserved, SIZE - 7, SIZE - 7);
        for (int i = 8; i < SIZE - 8; i++) {
            modules[6][i] = i % 2 == 0;
            modules[i][6] = i % 2 == 0;
            reserved[6][i] = true;
            reserved[i][6] = true;
        }

        byte[] bits = digestStream(content, SIZE * SIZE / 8 + 32);
        int bitIndex = 0;
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (!reserved[row][col]) {
                    boolean bit = ((bits[bitIndex / 8] >> (7 - bitIndex % 8)) & 1) == 1;
                    modules[row][col] = bit ^ ((row + col) % 2 == 0 && bitIndex % 5 == 0);
                    bitIndex++;
                }
            }
        }
        return modules;
    }

    private static void drawFinder(boolean[][] m, boolean[][] reserved, int top, int left) {
        for (int r = -1; r <= 7; r++) {
            for (int c = -1; c <= 7; c++) {
                int row = top + r;
                int col = left + c;
                if (row < 0 || col < 0 || row >= SIZE || col >= SIZE) {
                    continue;
                }
                boolean ring = r == 0 || r == 6 || c == 0 || c == 6;
                boolean core = r >= 2 && r <= 4 && c >= 2 && c <= 4;
                boolean inside = r >= 0 && r <= 6 && c >= 0 && c <= 6;
                m[row][col] = inside && (ring || core);
                reserved[row][col] = true;
            }
        }
    }

    private static void drawAlignment(boolean[][] m, boolean[][] reserved, int centerRow, int centerCol) {
        for (int r = -2; r <= 2; r++) {
            for (int c = -2; c <= 2; c++) {
                m[centerRow + r][centerCol + c] = Math.max(Math.abs(r), Math.abs(c)) != 1;
                reserved[centerRow + r][centerCol + c] = true;
            }
        }
    }

    /** Concatenates SHA-256(content + counter) blocks until {@code length} bytes are produced. */
    private static byte[] digestStream(String content, int length) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] out = new byte[length];
            int written = 0;
            for (int counter = 0; written < length; counter++) {
                byte[] block = sha.digest((content + "#" + counter).getBytes(StandardCharsets.UTF_8));
                int n = Math.min(block.length, length - written);
                System.arraycopy(block, 0, out, written, n);
                written += n;
            }
            return out;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
