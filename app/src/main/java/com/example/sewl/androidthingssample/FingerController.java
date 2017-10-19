package com.example.sewl.androidthingssample;

import android.os.Handler;

/**
 * Created by mderrick on 10/10/17.
 */

public class FingerController {

    public static final int SERVO_OFF_VALUE = 0;

    private static int FLEXED_ANGLE = 180;
    private static int LOOSE_ANGLE  = 0;

    private int channel;

    private int currentAngle = -1;

    private MultiChannelServoDriver servoDriver;

    private Handler settleServoHandler = new Handler();

    private Thread interpolationThread;

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
        setAngle(angle, true);
    }

    public void setAngle(int angle, boolean settle) {
        settleServoHandler.removeCallbacksAndMessages(null);
        if (servoDriver != null) {
            if (currentAngle != angle) {
                int diff = Math.abs(angle - currentAngle);
                servoDriver.setAngle(channel, angle);
                this.currentAngle = angle;
                if (settle) {
                    settleServo(diff);
                }
            }
        }
    }

    private void settleServo(int angleMoved) {
        settleServoHandler.removeCallbacksAndMessages(null);
        long relaxTime = (long) (((float) angleMoved / 180.0f) * 500.);
        settleServoHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                servoDriver.setPWM(channel, 0, SERVO_OFF_VALUE);
            }
        }, relaxTime);
    }

    private void interpolate() {
        interpolationThread = new Thread(new Runnable() {
            @Override
            public void run() {
            }
        });
        interpolationThread.start();
    }
}
