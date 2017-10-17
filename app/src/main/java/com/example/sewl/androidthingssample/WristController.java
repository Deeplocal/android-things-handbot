package com.example.sewl.androidthingssample;

import android.os.Handler;

/**
 * Created by mderrick on 10/17/17.
 */

public class WristController {

    private static int FLEXED_ANGLE = 180;
    private static int LOOSE_ANGLE  = 0;

    private int channel;

    private int currentAngle = -1;

    private MultiChannelServoDriver servoDriver;

    private Handler settleServoHandler = new Handler();

    public WristController(int channel, MultiChannelServoDriver servoDriver) {
        this.channel = channel;
        this.servoDriver = servoDriver;
    }

    public void perpendicularToGround() {
        if (currentAngle != FLEXED_ANGLE) {
            setAngle(FLEXED_ANGLE);
        }
    }

    public void parallelToGround() {
        if (currentAngle != LOOSE_ANGLE) {
            setAngle(LOOSE_ANGLE);
        }
    }

    public void setAngle(int angle) {
        settleServoHandler.removeCallbacksAndMessages(null);
        if (servoDriver != null) {
            if (currentAngle != angle) {
                servoDriver.setAngle(channel, angle);
            }
            this.currentAngle = angle;
            settleServo();
        }
    }

    private void settleServo() {
        settleServoHandler.removeCallbacksAndMessages(null);
        settleServoHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                servoDriver.setPWM(channel, 0, 0);
            }
        }, 1000);
    }
}
