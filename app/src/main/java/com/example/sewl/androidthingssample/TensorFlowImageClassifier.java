package com.example.sewl.androidthingssample;

import android.content.Context;
import android.graphics.Bitmap;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.List;

/**
 * Created by mderrick on 10/2/17.
 */

public class TensorFlowImageClassifier implements Classifier {

    private static final String TAG = "TFImageClassifier";

    private final String modelFile;

    private String[] labels;

    // Pre-allocated buffers.
    private float[] floatValues;
    private int[] intValues;
    private float[] outputs;

    private TensorFlowInferenceInterface inferenceInterface;

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param context The activity that instantiates this.
     */
    public TensorFlowImageClassifier(Context context, String modelFile, String labelFile) {
        this.inferenceInterface = new TensorFlowInferenceInterface(
                context.getAssets(),
                modelFile);
        this.labels = Helper.readLabels(context, labelFile);
        this.modelFile = modelFile;

        // Pre-allocate buffers.
        intValues = new int[Helper.IMAGE_SIZE * Helper.IMAGE_SIZE];
        floatValues = new float[Helper.IMAGE_SIZE * Helper.IMAGE_SIZE * 3];
        outputs = new float[Helper.NUM_CLASSES];
    }

    /**
     * Clean up the resources used by the classifier.
     */
    public void destroyClassifier() {
        inferenceInterface.close();
    }


    /**
     * @param image Bitmap containing the image to be classified. The image can be
     *              of any size, but preprocessing might occur to resize it to the
     *              format expected by the classification process, which can be time
     *              and power consuming.
     */
    public List<Classifier.Recognition> doRecognize(Bitmap image) {
        float[] pixels = Helper.getPixels(Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight()), intValues, floatValues);

        // Feed the pixels of the image into the TensorFlow Neural Network
        inferenceInterface.feed(Helper.INPUT_NAME, pixels,
                Helper.NETWORK_STRUCTURE);

        // Run the TensorFlow Neural Network with the provided input
        inferenceInterface.run(Helper.OUTPUT_NAMES);

        // Extract the output from the neural network back into an array of confidence per category
        inferenceInterface.fetch(Helper.OUTPUT_NAME, outputs);

        // Get the results with the highest confidence and map them to their labels
        return Helper.getBestResults(outputs, labels);
    }

    public String getModelFile() {
        return modelFile;
    }
}
