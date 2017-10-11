package com.example.sewl.androidthingssample;

import android.os.Handler;
import android.util.Log;

/**
 * Created by mderrick on 10/10/17.
 */

public class FingerController {

    private static int FLEXED_ANGLE = 180;
    private static int LOOSE_ANGLE  = 0;

    private int channel;

    private int currentAngle = -1;

    private MultiChannelServoDriver servoDriver;

    private Handler settleServoHandler = new Handler();

    public FingerController(int channel, MultiChannelServoDriver servoDriver) {
        this.channel = channel;
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
                Log.i("SET", "channel: " + channel + " " + angle);
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
