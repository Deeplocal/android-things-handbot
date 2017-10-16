package com.example.sewl.androidthingssample;

import android.app.Activity;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.google.android.things.pio.PeripheralManagerService;

import java.util.List;

import static android.content.ContentValues.TAG;

public class ImageClassifierActivity extends Activity
                                     implements ImageReader.OnImageAvailableListener,
                                                CameraHandler.CameraReadyListener,
                                                ImageClassificationAsyncTask.ClassificationAvailableListener {

    private static final int NUMBER_OF_LEDS = 4;
    private ImagePreprocessor imagePreprocessor;

    private CameraHandler mCameraHandler;

    private TensorFlowImageClassifier tensorFlowClassifier;

    private TensorFlowImageClassifier rpsTensorFlowClassifier;

    private HandlerThread mBackgroundThread;

    private Handler mBackgroundHandler;

    private HandController handController;

    private StandbyController standbyController;

    private Handler localHandler = new Handler();

    private HandlerThread mPioThread;

    private static final int FRAME_DELAY_MS = 100; // 10fps

    private LightRingControl lightRingControl;

    static class LocalHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            Log.i(this.getClass().getName(), "Message received: " + (String) msg.obj);
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        init();

        localHandler = new LocalHandler();

        lightRingControl = new LightRingControl(localHandler);
        standbyController = new StandbyController();
        standbyController.init(handController, lightRingControl);

        testApa102();
    }

    private void testApa102() {
        PeripheralManagerService manager = new PeripheralManagerService();
        List<String> deviceList = manager.getSpiBusList();
        if (deviceList.isEmpty()) {
            Log.i(TAG, "No SPI bus available on this device.");
        } else {
            Log.i(TAG, "List of available devices: " + deviceList);
        }
        lightRingControl.init();
//        lightRingControl.runPulse(5);
//        lightRingControl.setScore(2, 1);
    }

    private void init() {
        mBackgroundThread = new HandlerThread("BackgroundThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        mBackgroundHandler.post(mInitializeOnBackground);
        handController = new HandController();
//        handController.init();
    }

    private Runnable mInitializeOnBackground = new Runnable() {
        @Override
        public void run() {
            imagePreprocessor = new ImagePreprocessor();
            mCameraHandler = CameraHandler.getInstance();
            mCameraHandler.initializeCamera(
                    ImageClassifierActivity.this, mBackgroundHandler,
                    ImageClassifierActivity.this);
            mCameraHandler.setCameraReadyListener(ImageClassifierActivity.this);
            tensorFlowClassifier = new TensorFlowImageClassifier(ImageClassifierActivity.this, Helper.MODEL_FILE, Helper.LABELS_FILE);
            rpsTensorFlowClassifier = new TensorFlowImageClassifier(ImageClassifierActivity.this, Helper.RPS_MODEL_FILE, Helper.RPS_LABELS_FILE);
        }
    };

    @Override
    public void onImageClassificationAvailable(List<Classifier.Recognition> classifications) {
        if (classifications.size() > 0) {
            Log.i("ACTION", "action: " + classifications);
            if (standbyController != null) {
//                standbyController.run(classifications.get(0).getTitle());
            }
        }
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
//        new ImageClassificationAsyncTask(
//                imagePreprocessor, tensorFlowClassifier, this).execute(reader);
        new ImageClassificationAsyncTask(
                imagePreprocessor, rpsTensorFlowClassifier, this).execute(reader);
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
