package com.example.sewl.androidthingssample;

import android.os.Handler;

/**
 * Created by mderrick on 10/10/17.
 */

public class FingerController {

    public static final int SERVO_OFF_VALUE = 0;

    private static int FLEXED_ANGLE = 180;

    private static int LOOSE_ANGLE  = 0;

    private final int offset;

    private int channel;

    private int currentAngle = -1;

    private MultiChannelServoDriver servoDriver;

    private boolean reverseAngle;

    private Handler settleServoHandler = new Handler();

    public FingerController(int channel, MultiChannelServoDriver servoDriver, boolean reverseAngle, int offset) {
        this.channel = channel;
        this.servoDriver = servoDriver;
        this.reverseAngle = reverseAngle;
        this.offset = offset;
    }

    public void flex() {
        setAngle(FLEXED_ANGLE);
    }

    public void loose() {
        setAngle(LOOSE_ANGLE + offset);
    }

    public void setAngle(int angle) {
        int remapped = reverseAngle ? 180 - angle : angle;
        settleServoHandler.removeCallbacksAndMessages(null);
        if (servoDriver != null) {
            if (currentAngle != remapped) {
                int diff = Math.abs(remapped - currentAngle);
                servoDriver.setAngle(channel, remapped);
                this.currentAngle = remapped;
                if (angle == LOOSE_ANGLE) {
                   settleServo(diff);
                }
            }
        }
    }

    private void settleServo(int angleMoved) {
        settleServoHandler.removeCallbacksAndMessages(null);
        long relaxTime = (long) (((float) angleMoved / 180.0f) * 500.0f);
        settleServoHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                servoDriver.setPWM(channel, 0, SERVO_OFF_VALUE);
            }
        }, relaxTime);
    }
}
