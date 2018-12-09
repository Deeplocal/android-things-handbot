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

import com.google.android.things.device.TimeManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ImageClassifierActivity extends Activity
                                     implements ImageReader.OnImageAvailableListener,
                                                CameraHandler.CameraReadyListener {

    private static final String TAG = ImageClassifierActivity.class.getSimpleName();

    public static final int HAND_INIT_RPS_READY_TIME        = 600;
    public static final int HAND_INIT_MOVE_TO_LOOSE_TIME    = 1200;
    public static final int HAND_INIT_START_RUN_TIME        = 10000;
    public static final int FOREARM_TEST_MOVEMENT_DELAY     = 500;

    private Map<String, TensorFlowImageClassifier> classifiers = new HashMap();

    private ImageClassificationThread imageClassificationThread;

    private TensorFlowImageClassifier rpsTensorFlowClassifier;

    private TensorFlowImageClassifier spidermanOkClassifier;

    private TensorFlowImageClassifier loserThreeClassifier;

    private TensorFlowImageClassifier oneRockClassifier;

    private TensorFlowImageClassifier mirrorClassifier;

    private SettingsRepository settingsRepository;


    private ImagePreprocessor imagePreprocessor;

    private StandbyController standbyController;

    private LightRingControl lightRingControl;

    private States currentState = States.IDLE;

    private HandlerThread backgroundThread;

    private SoundController soundController;

    private HandController handController;

    private CameraHandler cameraHandler;

    private Handler backgroundHandler;

    private int keyPresses = 0;

    private enum States {
        IDLE,
        STARTUP,
        CONFIGURE,
        RUN
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_camera);

        init();
    }

    private void init() {
        wait(200);
        settingsRepository = new SettingsRepository(this);
        handController = new HandController();
        handController.init(settingsRepository);
        wait(200);
        handController.indexFinger.flex();
        wait(200);
        handController.indexFinger.loose();
        wait(200);
        handController.wrist.perpendicularToGround();
        wait(200);
        handController.wrist.parallelToGround();
        wait(200);
        handController.forearm.minorFlex();
        wait(200);
        handController.forearm.flex();
        wait(200);
        handController.forearm.loose();
        wait(200);

        standbyController = new StandbyController();
        soundController = new SoundController(this);
        lightRingControl = new LightRingControl();

        lightRingControl.init();


        imagePreprocessor = new ImagePreprocessor();
        // Create Tensorflow classifiers
       rpsTensorFlowClassifier = new TensorFlowImageClassifier(ImageClassifierActivity.this, TensorflowImageOperations.RPS_MODEL_FILE, TensorflowImageOperations.RPS_LABELS_FILE);
       spidermanOkClassifier = new TensorFlowImageClassifier(ImageClassifierActivity.this, TensorflowImageOperations.SPIDERMAN_OK_MODEL, TensorflowImageOperations.SPIDERMAN_OK_LABELS);
       loserThreeClassifier = new TensorFlowImageClassifier(ImageClassifierActivity.this, TensorflowImageOperations.LOSER_THREE_MODEL, TensorflowImageOperations.LOSER_THREE_LABELS);
       oneRockClassifier = new TensorFlowImageClassifier(ImageClassifierActivity.this, TensorflowImageOperations.ONE_ROCK_MODEL, TensorflowImageOperations.ONE_ROCK_LABELS);
       mirrorClassifier = new TensorFlowImageClassifier(ImageClassifierActivity.this, TensorflowImageOperations.MIRROR_MODEL, TensorflowImageOperations.MIRROR_LABELS);

        standbyController.init(handController, lightRingControl, soundController);


        // Use a different specific classifier for actions that don't play well together
        classifiers.put(Signs.SPIDERMAN, spidermanOkClassifier);
        classifiers.put(Signs.THREE, loserThreeClassifier);
        classifiers.put(Signs.OK, spidermanOkClassifier);
        classifiers.put(Signs.ROCK, rpsTensorFlowClassifier);
        classifiers.put(Signs.PAPER, rpsTensorFlowClassifier);
        classifiers.put(Signs.SCISSORS, rpsTensorFlowClassifier);
        classifiers.put(Signs.LOSER, loserThreeClassifier);
        classifiers.put(Signs.ONE, oneRockClassifier);
        classifiers.put(Signs.HANG_LOOSE, mirrorClassifier);
        classifiers.put("rps", rpsTensorFlowClassifier);
        classifiers.put("mirror", mirrorClassifier);
        classifiers.put("simon_says", rpsTensorFlowClassifier);

        backgroundThread = new HandlerThread("BackgroundThread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        backgroundHandler.post(mInitializeOnBackground);
        handController = new HandController();
        handController.init(settingsRepository);

        if (handController!=null) {
            runHandInit();
        }



        imageClassificationThread = new ImageClassificationThread(standbyController, classifiers, lightRingControl);
        imageClassificationThread.start();


        //setupButtons();

    }

    private void wait(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void runHandInit() {
        handController.loose();
        handController.moveToRPSReady(HAND_INIT_RPS_READY_TIME);
        handController.loose(HAND_INIT_MOVE_TO_LOOSE_TIME);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                currentState = States.RUN;
            }
        }, HAND_INIT_START_RUN_TIME);
    }

    private void setupButtons() {

       /*  try {
           buttonInputDriver = new ButtonInputDriver(BoardDefaults.CONFIG_BUTTON_GPIO, Button.LogicState.PRESSED_WHEN_HIGH, KeyEvent.KEYCODE_SPACE);
            resetButton = new ButtonInputDriver(BoardDefaults.RESET_BUTTON_GPIO, Button.LogicState.PRESSED_WHEN_HIGH, KeyEvent.KEYCODE_E);
            buttonInputDriver.register();
            resetButton.register();
        } catch (IOException e) {
            Log.e(TAG, "Failed to setup configuration buttons: " + e);
        }*/

    }

    private Runnable mInitializeOnBackground = new Runnable() {
        @Override
        public void run() {
            cameraHandler = CameraHandler.getInstance();
            cameraHandler.initializeCamera(
                    ImageClassifierActivity.this, backgroundHandler,
                    ImageClassifierActivity.this);
            cameraHandler.setCameraReadyListener(ImageClassifierActivity.this);
        }
    };

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            keyPresses++;
            if (keyPresses >= 3 && currentState == States.STARTUP) {
                currentState = States.CONFIGURE;
                lightRingControl.setColor(Color.CYAN);
                soundController.playSound(SoundController.Sounds.CORRECT);
                runFlexForearmTest();
            } else if (currentState == States.CONFIGURE) {
                settingsRepository.incrementForearmOffset();
                runFlexForearmTest();
            }
        } else if (keyCode == KeyEvent.KEYCODE_E) {
            if (imageClassificationThread != null && imageClassificationThread.isAlive()) {
                Message msg = new Message();
                msg.arg1 = ImageClassificationThread.RESET_CODE;
                imageClassificationThread.getHandler().sendMessage(msg);
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    private void runFlexForearmTest() {
        handController.forearm.flex();
        handController.loose(FOREARM_TEST_MOVEMENT_DELAY);
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
            cameraHandler.takePicture();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (backgroundThread != null) backgroundThread.quit();
        } catch (Throwable t) {
            Log.e(TAG, "Failed to quit the background thread: " + t);
        }

        handController.shutdown();
        backgroundThread = null;
        backgroundHandler = null;

        try {
            if (cameraHandler != null) cameraHandler.shutDown();
        } catch (Throwable t) {
            Log.e(TAG, "Failed to quit the camera thread: " + t);
        }
        try {
            if (rpsTensorFlowClassifier != null) rpsTensorFlowClassifier.destroyClassifier();
            if (spidermanOkClassifier != null) spidermanOkClassifier.destroyClassifier();
            if (loserThreeClassifier != null) loserThreeClassifier.destroyClassifier();
            if (oneRockClassifier != null) oneRockClassifier.destroyClassifier();
        } catch (Throwable t) {
            Log.e(TAG, "Failed to quit the classifier threads: " + t);
        }
    }

    @Override
    public void onCameraReady() {
        cameraHandler.takePicture();
    }
}
