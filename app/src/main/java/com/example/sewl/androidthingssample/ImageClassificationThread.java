package com.example.sewl.androidthingssample;

import android.graphics.Bitmap;
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

    private Handler handler;

    private long currentTime;

    private boolean classifyingImage;

    public ImageClassificationThread(TensorFlowImageClassifier imageClassifier, StandbyController standbyController) {
        super("imageClassificationThread");
        this.imageClassifier = imageClassifier;
        this.standbyController = standbyController;
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
        };        Looper.loop();

        Looper.myLooper().quit();
    }

    public void classifyImage(Bitmap bitmap) {
        Log.i("TOOK", "overall: " + (System.currentTimeMillis() - currentTime));
        currentTime = System.currentTimeMillis();

        final List<Classifier.Recognition> results = imageClassifier.doRecognize(bitmap);
        if (results.size() > 0) {
//            standbyController.run(results.get(0).getTitle(), results);
            Log.i("RESULTS", imageClassifier.getModelFile() + " : " + results);
        }
        classifyingImage = false;
    }

    public Handler getHandler() {
        return handler;
    }
}
