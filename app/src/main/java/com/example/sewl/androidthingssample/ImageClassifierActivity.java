package com.example.sewl.androidthingssample;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.KeyEvent;

import com.google.android.things.contrib.driver.button.ButtonInputDriver;

import java.util.HashMap;
import java.util.Map;

public class ImageClassifierActivity extends Activity
                                     implements ImageReader.OnImageAvailableListener,
                                                CameraHandler.CameraReadyListener {

    private static final String TAG = ImageClassifierActivity.class.getSimpleName();

    private ImagePreprocessor imagePreprocessor;

    private CameraHandler mCameraHandler;

    private TensorFlowImageClassifier tensorFlowClassifier;

    private TensorFlowImageClassifier rpsTensorFlowClassifier;

    private TensorFlowImageClassifier spidermanThreeClassifier;

    private TensorFlowImageClassifier loserOkNegativeClassifier;

    private HandlerThread mBackgroundThread;

    private Handler mBackgroundHandler;

    private HandController handController;

    private StandbyController standbyController;

    private LightRingControl lightRingControl;

    private SoundController soundController;

    private ImageClassificationThread imageClassificationThread;

    private ButtonInputDriver mButtonInputDriver;

    private Map<String, TensorFlowImageClassifier> classifiers = new HashMap();

    private int handClosed = 0;

    private boolean shouldContinueSim;

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
        handController = new HandController();
        handController.init();
        rpsTensorFlowClassifier = new TensorFlowImageClassifier(ImageClassifierActivity.this, Helper.RPS_MODEL_FILE, Helper.RPS_LABELS_FILE);
        spidermanThreeClassifier = new TensorFlowImageClassifier(ImageClassifierActivity.this, Helper.SPIDERMAN_THREE_MODEL, Helper.SPIDERMAN_THREE_LABELS);
        loserOkNegativeClassifier = new TensorFlowImageClassifier(ImageClassifierActivity.this, Helper.LOSER_OK_NEG_MODEL, Helper.LOSER_OK_NEG_LABELS);
        tensorFlowClassifier = new TensorFlowImageClassifier(ImageClassifierActivity.this, Helper.MODEL_FILE, Helper.LABELS_FILE);
        standbyController.init(handController, lightRingControl, soundController);

        classifiers.put(Signs.SPIDERMAN, spidermanThreeClassifier);
        classifiers.put(Signs.THREE, spidermanThreeClassifier);
        classifiers.put(Signs.ROCK, rpsTensorFlowClassifier);
        classifiers.put(Signs.PAPER, rpsTensorFlowClassifier);
        classifiers.put(Signs.SCISSORS, rpsTensorFlowClassifier);
        classifiers.put(Signs.LOSER, loserOkNegativeClassifier);
        classifiers.put(Signs.OK, loserOkNegativeClassifier);
        classifiers.put("rps", rpsTensorFlowClassifier);
        classifiers.put("mirror", rpsTensorFlowClassifier);
        classifiers.put("simon_says", rpsTensorFlowClassifier);

        mBackgroundThread = new HandlerThread("BackgroundThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        mBackgroundHandler.post(mInitializeOnBackground);

        imageClassificationThread = new ImageClassificationThread(standbyController, classifiers);
        imageClassificationThread.start();

        handController.mirrorRock();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                handController.loose();
            }
        }, 2000);
    }

    private Runnable mInitializeOnBackground = new Runnable() {
        @Override
        public void run() {
            mCameraHandler = CameraHandler.getInstance();
            mCameraHandler.initializeCamera(
                    ImageClassifierActivity.this, mBackgroundHandler,
                    ImageClassifierActivity.this);
            mCameraHandler.setCameraReadyListener(ImageClassifierActivity.this);
        }
    };

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        soundController.playSound(SoundController.SOUNDS.CORRECT);
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        if (imageClassificationThread != null && imageClassificationThread.isAlive()) {
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
