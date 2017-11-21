package com.example.sewl.androidthingssample;

import android.os.Handler;

/**
 * Created by mderrick on 10/17/17.
 */

public class ForearmController {

    public static int DEFAULT_MOTOR_SYNC_OFFSET  = 40;
    private static int FLEXED_ANGLE              = 20;
    private static int MINOR_FLEXED_ANGLE        = 10;
    private static int LOOSE_ANGLE               = -10;

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
        setAngle(LOOSE_ANGLE);
    }

    public void setAngle(int angle) {
        settleServoHandler.removeCallbacksAndMessages(null);
        if (servoDriver != null) {
            if (currentAngle != angle) {
                int currentMotorOffset = settingsRepository.getForearmServoOffset();
                int remappedAngle = angle + currentMotorOffset < 180 ? angle + currentMotorOffset : 180;
                servoDriver.setAngle(channel2, 180 - remappedAngle);
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
        }, 1000);
    }
}
