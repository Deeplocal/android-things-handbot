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
import java.util.HashMap;
import java.util.Map;

public class ImageClassifierActivity extends Activity
                                     implements ImageReader.OnImageAvailableListener,
                                                CameraHandler.CameraReadyListener {

    private static final String TAG = ImageClassifierActivity.class.getSimpleName();

    private ImagePreprocessor imagePreprocessor;

    private CameraHandler mCameraHandler;

    private TensorFlowImageClassifier rpsTensorFlowClassifier;

    private TensorFlowImageClassifier loserSpidermanClassifier;

    private TensorFlowImageClassifier okThreeClassifier;

    private HandlerThread mBackgroundThread;

    private Handler mBackgroundHandler;

    private HandController handController;

    private StandbyController standbyController;

    private LightRingControl lightRingControl;

    private SoundController soundController;

    private ImageClassificationThread imageClassificationThread;

    private ButtonInputDriver mButtonInputDriver;

    private Map<String, TensorFlowImageClassifier> classifiers = new HashMap();

    private int keyPresses = 0;

    private STATES currentState = STATES.IDLE;

    private SettingsRepository settingsRepository;

    private enum STATES {
        IDLE,
        STARTUP,
        CONFIGURE,
        RUN
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        init();
    }

    private void init() {
        settingsRepository = new SettingsRepository(this);
        standbyController = new StandbyController();
        soundController = new SoundController(this);
        lightRingControl = new LightRingControl();
        lightRingControl.init();
        imagePreprocessor = new ImagePreprocessor();
        handController = new HandController();
        handController.init(settingsRepository);
        rpsTensorFlowClassifier = new TensorFlowImageClassifier(ImageClassifierActivity.this, Helper.RPS_MODEL_FILE, Helper.RPS_LABELS_FILE);
        loserSpidermanClassifier = new TensorFlowImageClassifier(ImageClassifierActivity.this, Helper.LOSER_SPIDERMAN_MODEL, Helper.LOSER_SPIDERMAN_LABELS);
        okThreeClassifier = new TensorFlowImageClassifier(ImageClassifierActivity.this, Helper.OK_THREE_MODEL, Helper.OK_THREE_LABELS);
        standbyController.init(handController, lightRingControl, soundController);

        // Use a different specific classifier for actions that don't play well together
        classifiers.put(Signs.SPIDERMAN, loserSpidermanClassifier);
        classifiers.put(Signs.THREE, okThreeClassifier);
        classifiers.put(Signs.OK, okThreeClassifier);
        classifiers.put(Signs.ROCK, rpsTensorFlowClassifier);
        classifiers.put(Signs.PAPER, rpsTensorFlowClassifier);
        classifiers.put(Signs.SCISSORS, rpsTensorFlowClassifier);
        classifiers.put(Signs.LOSER, loserSpidermanClassifier);
        classifiers.put("rps", rpsTensorFlowClassifier);
        classifiers.put("mirror", rpsTensorFlowClassifier);
        classifiers.put("simon_says", rpsTensorFlowClassifier);

        mBackgroundThread = new HandlerThread("BackgroundThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        mBackgroundHandler.post(mInitializeOnBackground);

        imageClassificationThread = new ImageClassificationThread(standbyController, classifiers, lightRingControl);
        imageClassificationThread.start();

        lightRingControl.setColor(Color.BLACK);
        handController.loose();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                handController.moveToRPSReady();
            }
        }, 600);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                handController.loose();
            }
        }, 1200);

        try {
            mButtonInputDriver = new ButtonInputDriver("GPIO_33", Button.LogicState.PRESSED_WHEN_HIGH, KeyEvent.KEYCODE_SPACE);
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
        }
    };

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        keyPresses++;
        if (keyPresses >= 3 && currentState == STATES.STARTUP) {
            currentState = STATES.CONFIGURE;
            lightRingControl.setColor(Color.CYAN);
            soundController.playSound(SoundController.SOUNDS.CORRECT);
            runFlexForearmTest();
        } else if (currentState == STATES.CONFIGURE) {
            settingsRepository.incrementForearmOffset();
            runFlexForearmTest();
        }
        return super.onKeyUp(keyCode, event);
    }

    private void runFlexForearmTest() {
        handController.forearm.flex();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                handController.forearm.loose();
            }
        }, 500);
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
            if (rpsTensorFlowClassifier != null) rpsTensorFlowClassifier.destroyClassifier();
            if (loserSpidermanClassifier != null) loserSpidermanClassifier.destroyClassifier();
            if (okThreeClassifier != null) okThreeClassifier.destroyClassifier();
        } catch (Throwable t) { }
    }

    @Override
    public void onCameraReady() {
        mCameraHandler.takePicture();
    }
}
