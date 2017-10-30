package com.example.sewl.androidthingssample;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;

import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;

import java.io.IOException;

public class ImageClassifierActivity extends Activity
                                     implements ImageReader.OnImageAvailableListener,
                                                CameraHandler.CameraReadyListener {

    private static final String TAG = ImageClassifierActivity.class.getSimpleName();

    private ImagePreprocessor imagePreprocessor;

    private CameraHandler mCameraHandler;

    private TensorFlowImageClassifier tensorFlowClassifier;

    private TensorFlowImageClassifier rpsTensorFlowClassifier;

    private HandlerThread mBackgroundThread;

    private Handler mBackgroundHandler;

    private HandController handController;

    private StandbyController standbyController;

    private LightRingControl lightRingControl;

    private SoundController soundController;

    private ImageClassificationThread imageClassificationThread;

    private ButtonInputDriver mButtonInputDriver;

    private int handClosed = 0;

    private boolean processingImage;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        init();
    }

    private void init() {
        standbyController = new StandbyController();
        soundController = new SoundController(this);
        lightRingControl = new LightRingControl();
        lightRingControl.init();
        imagePreprocessor = new ImagePreprocessor();
        mBackgroundThread = new HandlerThread("BackgroundThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        mBackgroundHandler.post(mInitializeOnBackground);
        handController = new HandController();
        handController.init();
        rpsTensorFlowClassifier = new TensorFlowImageClassifier(ImageClassifierActivity.this, Helper.RPS_MODEL_FILE, Helper.RPS_LABELS_FILE);
        standbyController.init(handController, lightRingControl, soundController);
        imageClassificationThread = new ImageClassificationThread(rpsTensorFlowClassifier, standbyController);
        imageClassificationThread.start();

        // TODO: Remove me
        try {
            mButtonInputDriver = new ButtonInputDriver(
                    "GPIO_33",
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_SPACE);
            handController.loose();
            mButtonInputDriver.register();
        } catch (IOException e) {
            Log.e(TAG, "Error configuring GPIO pin", e);
        }
    }

    private Runnable mInitializeOnBackground = new Runnable() {
        @Override
        public void run() {
            mCameraHandler = CameraHandler.getInstance();
            mCameraHandler.initializeCamera(
                    ImageClassifierActivity.this, mBackgroundHandler,
                    ImageClassifierActivity.this);
            mCameraHandler.setCameraReadyListener(ImageClassifierActivity.this);
            tensorFlowClassifier = new TensorFlowImageClassifier(ImageClassifierActivity.this, Helper.MODEL_FILE, Helper.LABELS_FILE);
        }
    };

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
//        soundController.playSound(SoundController.SOUNDS.CORRECT);
        this.lightRingControl.runSwirl(1, Color.MAGENTA);
        handClosed++;
        if (handClosed == 1) {
//            handController.rock();
            handController.thumb.flex();
//        } else if (handClosed == 2) {
//            handController.one();
        } else {
            handController.loose();
            handClosed = 0;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        if (imageClassificationThread != null && imageClassificationThread.isAlive()) {
            processingImage = true;
            final Bitmap bitmap;
            try (Image image = reader.acquireLatestImage()) {
                bitmap = imagePreprocessor.preprocessImage(image);
            }
            if (bitmap != null) {
                Message message = new Message();
                message.obj = bitmap;
                imageClassificationThread.getHandler().sendMessage(message);
            }
            mCameraHandler.takePicture();
        }
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
            if (tensorFlowClassifier != null) tensorFlowClassifier.destroyClassifier();
        } catch (Throwable t) { }
    }

    @Override
    public void onCameraReady() {
        mCameraHandler.takePicture();
    }
}
