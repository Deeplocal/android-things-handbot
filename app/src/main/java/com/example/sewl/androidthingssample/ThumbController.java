package com.example.sewl.androidthingssample;

import android.os.Handler;

import com.sewl.deeplocal.drivers.MultiChannelServoDriver;

/**
 * Created by mderrick on 10/26/17.
 */

public class ThumbController {

    private static int FLEXED_ANGLE                 = 100;
    private static int LOOSE_ANGLE                  =200;
    public static final int SERVO_MAX_DEGREES       = 180;

    private int channel;

    private int currentAngle = -1;

    private MultiChannelServoDriver servoDriver;

    private Handler settleServoHandler = new Handler();

    private boolean reverseAngle;

    public ThumbController(int channel, MultiChannelServoDriver servoDriver, boolean reverseAngle) {
        this.channel = channel;
        this.servoDriver = servoDriver;
        this.reverseAngle = reverseAngle;
    }

    public void flex() {
        setAngle(FLEXED_ANGLE);
    }

    public void loose() {
        setAngle(LOOSE_ANGLE);
    }

    public void setAngle(int angle) {
        int remapped = reverseAngle ? SERVO_MAX_DEGREES - angle : angle;
        settleServoHandler.removeCallbacksAndMessages(null);
        if (servoDriver != null) {
            if (currentAngle != remapped) {
                servoDriver.setAngle(channel, remapped);
                this.currentAngle = remapped;
            }
        }
    }
}
