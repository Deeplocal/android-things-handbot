package com.example.sewl.androidthingssample;

import android.os.Handler;

import com.sewl.deeplocal.drivers.MultiChannelServoDriver;

/**
 * Created by mderrick on 10/10/17.
 */

public class FingerController {

    public static final int SERVO_OFF_VALUE         = 0;
    public static final int SERVO_MAX_DEGREES       = 180;
    public static final float MAX_RELAX_TIME_MILLIS = 500.0f;
    private static int FLEXED_ANGLE                 = 70;
    private static int LOOSE_ANGLE                  = 20;

    private MultiChannelServoDriver servoDriver;

    private boolean reverseAngle;

    private Handler settleServoHandler = new Handler();

    private final int offset;

    private int channel;

    private int currentAngle = -1;

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
        int remapped = reverseAngle ? SERVO_MAX_DEGREES - angle : angle;
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
        long relaxTime = (long) (((float) angleMoved / SERVO_MAX_DEGREES) * MAX_RELAX_TIME_MILLIS);
        settleServoHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                servoDriver.setPWM(channel, 0, SERVO_OFF_VALUE);
            }
        }, relaxTime);
    }
}
