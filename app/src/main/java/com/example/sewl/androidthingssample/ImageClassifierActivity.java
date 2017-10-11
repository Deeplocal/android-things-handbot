package com.example.sewl.androidthingssample;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.List;

public class ImageClassifierActivity extends Activity implements ImageReader.OnImageAvailableListener, CameraHandler.CameraReadyListener {

    private ImagePreprocessor mImagePreprocessor;

    private CameraHandler mCameraHandler;

    private TensorFlowImageClassifier mTensorFlowClassifier;

    private HandlerThread mBackgroundThread;

    private Handler mBackgroundHandler;

    private long currentTime;

    private HandController handController;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        init();
    }

    private void init() {
        mBackgroundThread = new HandlerThread("BackgroundThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        mBackgroundHandler.post(mInitializeOnBackground);
        handController = new HandController();
        handController.init();
        handController.handleAction("ok");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                handController.handleAction("rock");
            }
        }, 3000);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                handController.handleAction("scissors");
            }
        }, 5000);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                handController.handleAction("paper");
            }
        }, 7000);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                handController.relax();
            }
        }, 9000);
    }

    private Runnable mInitializeOnBackground = new Runnable() {
        @Override
        public void run() {
            mImagePreprocessor = new ImagePreprocessor();
            mCameraHandler = CameraHandler.getInstance();
            mCameraHandler.initializeCamera(
                    ImageClassifierActivity.this, mBackgroundHandler,
                    ImageClassifierActivity.this);
            mCameraHandler.setCameraReadyListener(ImageClassifierActivity.this);
            mTensorFlowClassifier = new TensorFlowImageClassifier(ImageClassifierActivity.this);
        }
    };

    @Override
    public void onImageAvailable(ImageReader reader) {
        final Bitmap bitmap;
        try (Image image = reader.acquireNextImage()) {
            bitmap = mImagePreprocessor.preprocessImage(image);
        }

        final List<Classifier.Recognition> results = mTensorFlowClassifier.doRecognize(bitmap);
        Log.i("RESULTS", "Took " + (System.currentTimeMillis() - currentTime) + " milliseconds.");
        if (results.size() > 0) {
            Log.i("ACTION", "action: " + results);
//            handController.handleAction(results.get(0).getTitle());
        }

        mCameraHandler.takePicture();
        currentTime = System.currentTimeMillis();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mBackgroundThread != null) mBackgroundThread.quit();
        } catch (Throwable t) { }

        handController.shutdown();
        mBackgroundThread = null;
        mBackgroundHandler = null;

        try {
            if (mCameraHandler != null) mCameraHandler.shutDown();
        } catch (Throwable t) { }
        try {
            if (mTensorFlowClassifier != null) mTensorFlowClassifier.destroyClassifier();
        } catch (Throwable t) { }
    }

    @Override
    public void onCameraReady() {
        mCameraHandler.takePicture();
    }
}
