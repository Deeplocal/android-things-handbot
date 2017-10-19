package com.example.sewl.androidthingssample;

import android.os.Handler;

/**
 * Created by mderrick on 10/17/17.
 */

public class ForearmController {

    private static int FLEXED_ANGLE = 90;
    private static int LOOSE_ANGLE  = 0;

    private int channel1;

    private int channel2;

    private int currentAngle = -1;

    private MultiChannelServoDriver servoDriver;

    private Handler settleServoHandler = new Handler();

    public ForearmController(int channel1, int channel2, MultiChannelServoDriver servoDriver) {
        this.channel1 = channel1;
        this.channel2 = channel2;
        this.servoDriver = servoDriver;
    }

    public void flex() {
        if (currentAngle != FLEXED_ANGLE) {
            setAngle(FLEXED_ANGLE);
        }
    }

    public void loose() {
        if (currentAngle != LOOSE_ANGLE) {
            setAngle(LOOSE_ANGLE);
        }
    }

    public void setAngle(int angle) {
        settleServoHandler.removeCallbacksAndMessages(null);
        if (servoDriver != null) {
            if (currentAngle != angle) {
                servoDriver.setAngle(channel1, angle);
                servoDriver.setAngle(channel2, angle);
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
                servoDriver.setPWM(channel1, 0, 0);
                servoDriver.setPWM(channel2, 0, 0);
            }
        }, 1000);
    }
}
