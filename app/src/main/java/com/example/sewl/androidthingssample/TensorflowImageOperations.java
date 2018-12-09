package com.example.sewl.androidthingssample;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;

import junit.framework.Assert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Created by mderrick on 10/2/17.
 */

public class TensorflowImageOperations {

    public static final int IMAGE_SIZE                  = 128;
    public static final int IMAGE_MEAN                  = 0;
    public static final float IMAGE_STD                 = 255;
    public static final String RPS_LABELS_FILE          = "retrained_labels_rps_random_color.txt";
    public static final String SPIDERMAN_OK_LABELS      = "retrained_labels_spiderman_ok_random_color.txt";
    public static final String ONE_ROCK_LABELS          = "retrained_labels_one_rock_random_color.txt";
    public static final String LOSER_THREE_LABELS       = "retrained_labels_loser_three_random_color.txt";
    public static final String MIRROR_LABELS            = "retrained_labels_mirror_random_color_8000.txt";
    public static final String RPS_MODEL_FILE           = "file:///android_asset/retrained_graph_rps_random_color.pb";
    public static final String SPIDERMAN_OK_MODEL       = "file:///android_asset/retrained_graph_spiderman_ok_random_color.pb";
    public static final String ONE_ROCK_MODEL           = "file:///android_asset/retrained_graph_one_rock_random_color.pb";
    public static final String LOSER_THREE_MODEL        = "file:///android_asset/retrained_graph_loser_three_random_color.pb";
    public static final String MIRROR_MODEL             = "file:///android_asset/retrained_graph_mirror_random_color_8000.pb";
    public static final String INPUT_NAME               = "input:0";
    public static final String OUTPUT_OPERATION         = "final_result";
    public static final String OUTPUT_NAME              = OUTPUT_OPERATION + ":0";
    public static final String[] OUTPUT_NAMES           = { OUTPUT_NAME };
    public static final long[] NETWORK_STRUCTURE        = {1, IMAGE_SIZE, IMAGE_SIZE, 3};
    public static final int NUM_CLASSES                 = 1008;

    private static final int MAX_BEST_RESULTS           = 3;
    private static final float RES_CONFIDENCE_THRESHOLD = 0.1f;

    public static String[] readLabels(Context context, String labelFile) {
        AssetManager assetManager = context.getAssets();
        ArrayList<String> result = new ArrayList<>();
        try (InputStream is = assetManager.open(labelFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                result.add(line);
            }
            return result.toArray(new String[result.size()]);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read labels from " + labelFile);
        }
    }

    public static List<Classifier.Recognition> getBestResults(float[] confidenceLevels, String[] labels) {
        // Find the best classifications.
        PriorityQueue<Classifier.Recognition> pq = new PriorityQueue<>(MAX_BEST_RESULTS,
                new Comparator<Classifier.Recognition>() {
                    @Override
                    public int compare(Classifier.Recognition lhs, Classifier.Recognition rhs) {
                        // Intentionally reversed to put high confidence at the head of the queue.
                        return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                    }
                });

        for (int i = 0; i < confidenceLevels.length; ++i) {
            if (confidenceLevels[i] > RES_CONFIDENCE_THRESHOLD) {
                pq.add(new Classifier.Recognition("" + i, labels[i], confidenceLevels[i]));
            }
        }

        ArrayList<Classifier.Recognition> recognitions = new ArrayList();
        int recognitionsSize = Math.min(pq.size(), MAX_BEST_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }
        return recognitions;
    }

    public static float[] getPixels(Bitmap bitmap, int[] intValues, float[] floatValues) {
        if (bitmap.getWidth() != IMAGE_SIZE || bitmap.getHeight() != IMAGE_SIZE) {
            // rescale the bitmap if needed
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, IMAGE_SIZE, IMAGE_SIZE);
        }

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3] = (((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            floatValues[i * 3 + 2] = ((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
        }
        return floatValues;
    }

    public static void cropAndRescaleBitmap(final Bitmap src, final Bitmap dst, int sensorOrientation) {
        Assert.assertEquals(dst.getWidth(), dst.getHeight());
        final float minDim = Math.min(src.getWidth(), src.getHeight());

        final Matrix matrix = new Matrix();

        // We only want the center square out of the original rectangle.
        final float translateX = -Math.max(0, (src.getWidth() - minDim) / 2);
        final float translateY = -Math.max(0, (src.getHeight() - minDim) / 2);
        matrix.preTranslate(translateX, translateY);

        final float scaleFactor = dst.getHeight() / minDim;
        matrix.postScale(scaleFactor, scaleFactor);

        // Rotate around the center if necessary.
        if (sensorOrientation != 0) {
            matrix.postTranslate(-dst.getWidth() / 2.0f, -dst.getHeight() / 2.0f);
            matrix.postRotate(sensorOrientation);
            matrix.postTranslate(dst.getWidth() / 2.0f, dst.getHeight() / 2.0f);
        }

        final Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, matrix, null);
    }
}
