package com.example.sewl.androidthingssample;

import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.List;

/**
 * Created by mderrick on 10/16/17.
 */

public class ImageClassificationThread extends Thread {

    private final StandbyController standbyController;

    private TensorFlowImageClassifier imageClassifier;

    private ImagePreprocessor imagePreprocessor;

    private Handler handler;

    public ImageClassificationThread(TensorFlowImageClassifier imageClassifier, ImagePreprocessor imagePreprocessor, StandbyController standbyController) {
        super("imageClassificationThread");
        this.imageClassifier = imageClassifier;
        this.imagePreprocessor = imagePreprocessor;
        this.standbyController = standbyController;
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                classifyImage((ImageReader) msg.obj);
            }
        };
        Looper.loop();

        Looper.myLooper().quit();
    }

    private void classifyImage(ImageReader reader) {
        final Bitmap bitmap;
        try (Image image = reader.acquireLatestImage()) {
            bitmap = imagePreprocessor.preprocessImage(image);
        }
        final List<Classifier.Recognition> results = imageClassifier.doRecognize(bitmap);
        if (results.size() > 0) {
            standbyController.run(results.get(0).getTitle());
            Log.i("RESULTS", imageClassifier.getModelFile() + " : " + results);
        }
    }

    public Handler getHandler() {
        return handler;
    }
}
