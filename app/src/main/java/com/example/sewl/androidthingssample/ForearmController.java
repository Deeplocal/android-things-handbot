package com.example.sewl.androidthingssample;

import android.os.Handler;

import com.sewl.deeplocal.drivers.MultiChannelServoDriver;

/**
 * Created by mderrick on 10/17/17.
 */

public class ForearmController {

    public static int DEFAULT_MOTOR_SYNC_OFFSET     = 9;
    private static int FLEXED_ANGLE                 = 45;
    private static int MINOR_FLEXED_ANGLE           = 22;
    private static int LOOSE_ANGLE                  = 8;
    public static final int SERVO_MAX_DEGREES       = 180;
    public static final long MAX_RELAX_TIME_MILLIS  = 1000;

    private int channel1;

    private int channel2;

    private int currentAngle = 0;

    private MultiChannelServoDriver servoDriver;

    private Handler settleServoHandler = new Handler();

    private SettingsRepository settingsRepository;

    public ForearmController(int channel1, int channel2, MultiChannelServoDriver servoDriver, SettingsRepository settingsRepository) {
        this.channel1 = channel1;
        this.channel2 = channel2;
        this.servoDriver = servoDriver;
        this.settingsRepository = settingsRepository;
    }

    public void flex() {
        setAngle(FLEXED_ANGLE);
    }

    public void minorFlex() {
        setAngle(MINOR_FLEXED_ANGLE);
    }

    public void loose() {
        setAngle(LOOSE_ANGLE+1);
        setAngle(LOOSE_ANGLE);
    }

    public void setAngle(int angle) {
        settleServoHandler.removeCallbacksAndMessages(null);
        if (servoDriver != null) {
            if (currentAngle != angle) {
                int currentMotorOffset = settingsRepository.getForearmServoOffset();
                int remappedAngle = angle + currentMotorOffset < SERVO_MAX_DEGREES ? angle + currentMotorOffset : SERVO_MAX_DEGREES;
                servoDriver.setAngle(channel2, SERVO_MAX_DEGREES - remappedAngle);
                servoDriver.setAngle(channel1, angle);
            }
            this.currentAngle = angle;
            if (angle == LOOSE_ANGLE) {
                settleServo();
            }
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
        }, MAX_RELAX_TIME_MILLIS);
    }
}
