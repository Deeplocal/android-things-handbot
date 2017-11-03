package com.example.sewl.androidthingssample;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.List;
import java.util.Map;

/**
 * Created by mderrick on 10/16/17.
 */

public class ImageClassificationThread extends Thread {

    private final Map<String, TensorFlowImageClassifier> classifiers;

    private final StandbyController standbyController;

    private Handler handler;

    private long currentTime;

    private boolean classifyingImage;

    private String action;

    public ImageClassificationThread(StandbyController standbyController, Map<String, TensorFlowImageClassifier> classifiers) {
        super("imageClassificationThread");
        this.standbyController = standbyController;
        this.action = action;
        this.classifiers = classifiers;
        currentTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (!classifyingImage) {
                    classifyImage((Bitmap) msg.obj);
                }
            }
        };
        Looper.loop();

        Looper.myLooper().quit();
    }

    public void classifyImage(Bitmap bitmap) {
        Log.i("TOOK", "overall: " + (System.currentTimeMillis() - currentTime));
        currentTime = System.currentTimeMillis();

        TensorFlowImageClassifier classifier = classifiers.get(standbyController.getClassifierKey());
        if (classifier != null) {
            final List<Classifier.Recognition> results = classifier.doRecognize(bitmap);
            if (results.size() > 0) {
                standbyController.run(results.get(0).getTitle(), results);
                Log.i("RESULTS", classifier.getModelFile() + " : " + results);
            }
            classifyingImage = false;
        }
    }

    public Handler getHandler() {
        return handler;
    }
}
