package com.example.sewl.androidthingssample;

import android.os.Handler;

/**
 * Created by mderrick on 10/10/17.
 */

public class HandController {

    public static final int PWM_FREQUENCY = 60;

    private FingerController indexFinger;

    private FingerController ringFinger;

    private FingerController middleFinger;

    private FingerController pinky;

    private FingerController thumb;

    private ForearmController forearm;

    private MultiChannelServoDriver pwmDriver;

    private WristController wrist;

    private Thread interpolationThread;

    private FingerController[] orderedFingers;

    public void init() {
        pwmDriver = new MultiChannelServoDriver();
        pwmDriver.init(PWM_FREQUENCY);
        thumb = new FingerController(7, pwmDriver);
        indexFinger = new FingerController(6, pwmDriver);
        middleFinger = new FingerController(5, pwmDriver);
        ringFinger = new FingerController(4, pwmDriver);
        pinky = new FingerController(3, pwmDriver);
        wrist = new WristController(2, pwmDriver);
        forearm = new ForearmController(0, 1, pwmDriver);
        orderedFingers = new FingerController[] {
            indexFinger, middleFinger, ringFinger, pinky
        };
    }

    public void handleAction(String action) {
        if (action.contains("rock")) {
            moveToRPSReady();
        } else if (action.contains("scissors")) {
            scissors();
        } else if (action.contains("paper")) {
            relax();
        } else if (action.contains("ok")) {
            ok();
        }
    }

    public void runMirror(String action) {
        if (action.contains("rock")) {
            moveToRPSReady();
        } else if (action.contains("scissors")) {
            scissors();
        } else if (action.contains("ok")) {
            ok();
        }
    }

    public void scissors() {
        indexFinger.loose();
        middleFinger.loose();
        ringFinger.flex();
        pinky.flex();
        thumb.flex();
        forearm.flex();
        wrist.perpendicularToGround();
    }

    public void rock() {
        indexFinger.flex();
        middleFinger.flex();
        ringFinger.flex();
        pinky.flex();
        thumb.flex();
        forearm.flex();
        wrist.perpendicularToGround();
    }

    public void paper() {
        indexFinger.loose();
        middleFinger.loose();
        ringFinger.loose();
        pinky.loose();
        thumb.loose();
        forearm.flex();
        wrist.parallelToGround();
    }

    public void ok() {
        middleFinger.setAngle(80);
        ringFinger.setAngle(100);
        pinky.setAngle(140);
        indexFinger.flex();
        thumb.flex();
    }

    public void moveToRPSReady() {
        indexFinger.flex();
        middleFinger.flex();
        ringFinger.flex();
        pinky.flex();
        thumb.flex();
        forearm.loose();
        wrist.perpendicularToGround();
    }

    public void relax() {
        middleFinger.setAngle(110);
        ringFinger.setAngle(120);
        pinky.setAngle(140);
        indexFinger.setAngle(60);
        thumb.setAngle(140);
        wrist.parallelToGround();
        forearm.loose();
    }

    public void loose() {
        middleFinger.loose();
        ringFinger.loose();
        pinky.loose();
        indexFinger.loose();
        thumb.loose();
        wrist.parallelToGround();
        forearm.loose();
    }

    public void one() {
        indexFinger.loose();
        middleFinger.flex();
        ringFinger.flex();
        pinky.flex();
    }

    public void two() {
        indexFinger.loose();
        middleFinger.loose();
        ringFinger.flex();
        pinky.flex();
    }

    public void three() {
        indexFinger.loose();
        ringFinger.loose();
        middleFinger.loose();
        pinky.flex();
    }

    public void four() {
        relax();
    }

    public void test() {
        three();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                two();
            }
        }, 100);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                one();
            }
        }, 200);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                moveToRPSReady();
            }
        }, 300);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                one();
            }
        }, 700);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                two();
            }
        }, 800);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                three();
            }
        }, 900);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                relax();
            }
        }, 1000);
    }

    public void throwRPSAction(String action) {

    }

    public void interpolateToAngle() {
        interpolationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i+=4) {

                    for (int j = 0; j < orderedFingers.length; j++) {
                        float t = (float) i/100;
                        float offset = (float)j / (float) 4;
                        int angle = (int) Math.round(180 * Math.sin(t * Math.PI - offset));
                        orderedFingers[j].setAngle(angle, false);
                    }

                    try {
                        Thread.sleep(15);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                stopInterpolation();
            }
        });
        interpolationThread.start();
    }

    private void stopInterpolation() {
        if (interpolationThread != null) {
            interpolationThread.interrupt();
        }
    }

    public void thumbsUp() {
        moveToRPSReady();
    }

    public void thumbsDown() {
        moveToRPSReady();
    }

    public void shutdown() {
        pwmDriver.shutDown();
    }

    public void moveToIdle() {

    }
}
