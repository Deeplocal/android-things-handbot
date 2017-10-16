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

    private MultiChannelServoDriver pwmDriver;

    public void init() {
        pwmDriver = new MultiChannelServoDriver();
        pwmDriver.init(PWM_FREQUENCY);
        indexFinger = new FingerController(0, pwmDriver);
        ringFinger = new FingerController(2, pwmDriver);
        middleFinger = new FingerController(1, pwmDriver);
        pinky = new FingerController(3, pwmDriver);
        thumb = new FingerController(0, pwmDriver);
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

    private void scissors() {
//        indexFinger.loose();
//        middleFinger.loose();
//        ringFinger.flex();
//        pinky.flex();
    }

    public void ok() {
        middleFinger.setAngle(80);
        ringFinger.setAngle(100);
        pinky.setAngle(140);
        indexFinger.flex();
    }

    public void moveToRPSReady() {
        indexFinger.flex();
        ringFinger.flex();
        middleFinger.flex();
        pinky.flex();
    }

    public void relax() {
        indexFinger.loose();
        ringFinger.loose();
        middleFinger.loose();
        pinky.loose();
    }

    public void one() {
        indexFinger.loose();
        middleFinger.flex();
        ringFinger.flex();
        pinky.flex();
    }

    public void two() {
//        indexFinger.loose();
//        middleFinger.loose();
//        ringFinger.flex();
//        pinky.flex();
    }

    public void three() {
//        indexFinger.loose();
//        ringFinger.loose();
//        middleFinger.loose();
//        pinky.flex();
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
