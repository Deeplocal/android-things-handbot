package com.example.sewl.androidthingssample;

/**
 * Created by mderrick on 10/10/17.
 */

public class HandController {

    public static final int PWM_FREQUENCY = 60;

    private FingerController indexFinger;

    private FingerController ringFinger;

    private FingerController middleFinger;

    private FingerController pinky;

    private FingerController thumb;

    private MultiChannelServoDriver pwmDriver;

    public void init() {
        pwmDriver = new MultiChannelServoDriver();
        pwmDriver.init(PWM_FREQUENCY);
        indexFinger = new FingerController(0, pwmDriver);
        ringFinger = new FingerController(2, pwmDriver);
        middleFinger = new FingerController(1, pwmDriver);
        pinky = new FingerController(3, pwmDriver);
        thumb = new FingerController(0, pwmDriver);
    }

    public void handleAction(String action) {
        if (action.contains("rock")) {
            moveToRPSReady();
        } else if (action.contains("scissors")) {
            scissors();
        } else if (action.contains("paper")) {
            relax();
        } else if (action.contains("ok")) {
            ok();
        }
    }

    private void scissors() {
//        indexFinger.loose();
//        middleFinger.loose();
//        ringFinger.flex();
//        pinky.flex();
    }

    private void ok() {
//        middleFinger.setAngle(80);
//        ringFinger.setAngle(100);
//        pinky.setAngle(140);
//        indexFinger.flex();
    }

    public void moveToRPSReady() {
//        indexFinger.flex();
//        ringFinger.flex();
//        middleFinger.flex();
//        pinky.flex();
    }

    public void relax() {
//        indexFinger.loose();
//        ringFinger.loose();
//        middleFinger.loose();
//        pinky.loose();
    }

    public void one() {
//        indexFinger.loose();
//        ringFinger.flex();
//        middleFinger.flex();
//        pinky.flex();
    }

    public void three() {
//        indexFinger.loose();
//        ringFinger.loose();
//        middleFinger.loose();
//        pinky.loose();
    }

    public void throwRPSAction(String action) {

    }

    public void thumbsUp() {
//        moveToRPSReady();
    }

    public void thumbsDown() {
//        moveToRPSReady();
    }

    public void shutdown() {
//        pwmDriver.shutDown();
    }

    public void moveToIdle() {

    }
}
