package com.example.sewl.androidthingssample;

/**
 * Created by mderrick on 11/9/17.
 */

public class ImageFilters {

    private static int[][] SOBEL_X = new int[][] {
        { -1, 0, 1 },
        { -2, 0, 2 },
        { -1, 0, 1 }
    };

    private static int[][] SOBEL_Y = new int[][] {
        { -1, -2, -1 },
        { 0, 0, 20 },
        { 1, 2, 1 }
    };

    private static int[][] LAPLACIAN = new int[][] {
            { 0, -1, 0 },
            { -1, 4, -1 },
            { 0, -1, 0 }
    };

    public static int filter(int[] values, int i, int width) {
        int x = i % width;
        int y = (int) ((float) i / (float) width);
        float result = (LAPLACIAN[0][0] * grayValueAt(width, values, x - 1, y - 1)) + (LAPLACIAN[0][1] * grayValueAt(width, values, x, y - 1)) + (LAPLACIAN[0][2] * grayValueAt(width, values, x + 1, y - 1)) +
                (LAPLACIAN[1][0] * grayValueAt(width, values, x - 1, y)) + (LAPLACIAN[1][1] * grayValueAt(width, values, x, y)) + (LAPLACIAN[1][2] * grayValueAt(width, values, x + 1, y)) +
                (LAPLACIAN[2][0] * grayValueAt(width, values, x - 1, y + 1)) + (LAPLACIAN[2][1] * grayValueAt(width, values, x, y + 1)) + (LAPLACIAN[2][2] * grayValueAt(width, values, x + 1, y + 1));
        return Math.round(result);
    }

    private static float grayValueAt(int width, int[] values, int x, int y) {
        int i = x + y * width;
        if (i < 0 || i > values.length - 1) {
            return 0;
        }
        float r =  (((values[i] >> 16) & 0xFF) * 0.3f);
        float g =  (((values[i] >> 8) & 0xFF) * 0.59f);
        float b =  (((values[i] >> 0) & 0xFF) * 0.11f);
        float gray = r + g + b;
        return gray;
    }
}
