package com.example.sewl.androidthingssample;

/**
 * Created by mderrick on 10/10/17.
 */

public class HandController {

    public static final int PWM_FREQUENCY = 60;

    public FingerController indexFinger;

    public FingerController ringFinger;

    public FingerController middleFinger;

    public FingerController pinky;

    public ThumbController thumb;

    public ForearmController forearm;

    public MultiChannelServoDriver pwmDriver;

    public WristController wrist;

    public void init() {
        pwmDriver = new MultiChannelServoDriver();
        pwmDriver.init(PWM_FREQUENCY);
        thumb = new ThumbController(6, pwmDriver, true);
        indexFinger = new FingerController(5, pwmDriver, false, 0);
        middleFinger = new FingerController(4, pwmDriver, false, 0);
        ringFinger = new FingerController(3, pwmDriver, true, 40);
        pinky = new FingerController(2, pwmDriver, false, 0);
        wrist = new WristController(1, pwmDriver);
        forearm = new ForearmController(0, 0, pwmDriver);
    }

    public void handleAction(String action) {
        if (action.contains("rock")) {
            rock();
        } else if (action.contains("scissors")) {
            scissors();
        } else if (action.contains("paper")) {
            paper();
        } else if (action.contains("ok")) {
            ok();
        }
    }

    public void runMirror(String action) {
        if (action.contains("rock")) {
            fist();
        } else if (action.contains("scissors")) {
            mirrorScissors();
        } else if (action.contains("paper")) {
            mirrorPaper();
        }
    }

    public void scissors() {
        indexFinger.loose();
        middleFinger.loose();
        ringFinger.flex();
        pinky.flex();
        thumb.flex();
        forearm.flex();
        wrist.perpendicularToGround();
    }

    public void mirrorScissors() {
        indexFinger.loose();
        middleFinger.loose();
        ringFinger.flex();
        pinky.flex();
        thumb.flex();
        forearm.flex();
        wrist.parallelToGround();
    }

    public void rock() {
        indexFinger.flex();
        middleFinger.flex();
        ringFinger.flex();
        pinky.flex();
        thumb.flex();
        forearm.flex();
        wrist.perpendicularToGround();
    }

    public void fist() {
        indexFinger.flex();
        middleFinger.flex();
        ringFinger.flex();
        pinky.flex();
        thumb.flex();
        forearm.flex();
        wrist.parallelToGround();
    }

    public void paper() {
        indexFinger.loose();
        middleFinger.loose();
        ringFinger.loose();
        pinky.loose();
        thumb.loose();
        forearm.flex();
        wrist.perpendicularToGround();
    }

    public void mirrorPaper() {
        indexFinger.loose();
        middleFinger.loose();
        ringFinger.loose();
        pinky.loose();
        thumb.loose();
        forearm.loose();
        wrist.parallelToGround();
    }

    public void ok() {
        middleFinger.setAngle(80);
        ringFinger.setAngle(100);
        pinky.setAngle(140);
        indexFinger.flex();
        thumb.flex();
    }

    public void moveToRPSReady() {
        indexFinger.flex();
        middleFinger.flex();
        ringFinger.flex();
        pinky.flex();
        thumb.flex();
        forearm.flex();
        wrist.parallelToGround();
    }

    public void relax() {
        middleFinger.setAngle(110);
        ringFinger.setAngle(120);
        pinky.setAngle(140);
        indexFinger.setAngle(60);
        thumb.setAngle(140);
        wrist.parallelToGround();
        forearm.loose();
    }

    public void loose() {
        middleFinger.loose();
        ringFinger.loose();
        pinky.loose();
        indexFinger.loose();
        thumb.loose();
        wrist.parallelToGround();
        forearm.loose();
    }

    public void one() {
        indexFinger.loose();
        middleFinger.flex();
        ringFinger.flex();
        pinky.flex();
    }

    public void two() {
        indexFinger.loose();
        middleFinger.loose();
        ringFinger.flex();
        pinky.flex();
    }

    public void three() {
        indexFinger.loose();
        ringFinger.loose();
        middleFinger.loose();
        pinky.flex();
    }

    public void four() {
        relax();
    }

    public void thumbsUp() {
//        moveToRPSReady();
    }

    public void thumbsDown() {
        moveToRPSReady();
    }

    public void shutdown() {
        pwmDriver.shutDown();
    }

    public void moveToIdle() {

    }
}
