package com.example.sewl.androidthingssample;

import android.os.Handler;

/**
 * Created by mderrick on 10/17/17.
 */

public class WristController {

    private static int FLEXED_ANGLE             = 0;
    private static int LOOSE_ANGLE              = 112;
    public static final int SERVO_OFF_VALUE     = 0;

    private int channel;

    private int currentAngle = -1;

    private MultiChannelServoDriver servoDriver;

    private Handler settleServoHandler = new Handler();

    private boolean isEnabled = true;

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
        if (!isEnabled) {
            return;
        }

        settleServoHandler.removeCallbacksAndMessages(null);
        if (servoDriver != null) {
            int diff = Math.abs(angle - currentAngle);
            if (currentAngle != angle) {
                servoDriver.setAngle(channel, angle);
            }
            this.currentAngle = angle;
            settleServo(diff);
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
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
