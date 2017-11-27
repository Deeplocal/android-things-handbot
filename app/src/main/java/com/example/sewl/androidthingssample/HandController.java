package com.example.sewl.androidthingssample;

/**
 * Created by mderrick on 10/10/17.
 */

public class HandController {

    public FingerController indexFinger;

    public FingerController ringFinger;

    public FingerController middleFinger;

    public FingerController pinky;

    public ThumbController thumb;

    public ForearmController forearm;

    public MultiChannelServoDriver pwmDriver;

    public WristController wrist;

    public void init(SettingsRepository settingsRepository) {
        pwmDriver = new MultiChannelServoDriver();
        pwmDriver.init();

        thumb = new ThumbController(BoardDefaults.HandPinout.THUMB, pwmDriver, true);
        indexFinger = new FingerController(BoardDefaults.HandPinout.INDEX, pwmDriver, false, 0);
        middleFinger = new FingerController(BoardDefaults.HandPinout.MIDDLE, pwmDriver, false, 0);
        ringFinger = new FingerController(BoardDefaults.HandPinout.RING, pwmDriver, false, 0);
        pinky = new FingerController(BoardDefaults.HandPinout.PINKY, pwmDriver, false, 0);

        wrist = new WristController(BoardDefaults.HandPinout.WRIST, pwmDriver);
        forearm = new ForearmController(BoardDefaults.HandPinout.FOREARM_ON_USER_RIGHT, BoardDefaults.HandPinout.FOREARM_ON_USER_LEFT, pwmDriver, settingsRepository);
    }

    public void handleRPSAction(String action) {
        switch (action) {
            case Signs.ROCK:
                rock();
                break;
            case Signs.SCISSORS:
                scissors();
                break;
            case Signs.PAPER:
                paper();
                break;
        }
    }

    public void handleSimonSaysAction(String action) {
        switch (action) {
            case Signs.ROCK:
                mirrorRock();
                break;
            case Signs.SCISSORS:
                mirrorScissors();
                break;
            case Signs.PAPER:
                mirrorPaper();
                break;
            case Signs.SPIDERMAN:
                spiderman();
                break;
            case Signs.OK:
                ok();
                break;
            case Signs.ONE:
                ok();
                break;
            case Signs.THREE:
                three();
                break;
            case Signs.LOSER:
                loser();
                break;
            case Signs.HANG_LOOSE:
                loser();
                break;
        }
    }

    public void runMirror(String action) {
        switch (action) {
            case Signs.ROCK:
                mirrorRock();
                break;
            case Signs.SCISSORS:
                mirrorScissors();
                break;
            case Signs.HANG_LOOSE:
                hangLoose();
                break;
        }
    }

    private void hangLoose() {
        indexFinger.flex();
        middleFinger.flex();
        thumb.loose();
        ringFinger.flex();
        pinky.loose();
        forearm.loose();
        wrist.parallelToGround();
    }

    private void spiderman() {
        indexFinger.loose();
        middleFinger.flex();
        thumb.loose();
        ringFinger.flex();
        pinky.loose();
        forearm.loose();
        wrist.parallelToGround();
    }

    private void loser() {
        indexFinger.loose();
        middleFinger.flex();
        thumb.loose();
        ringFinger.flex();
        pinky.flex();
        forearm.loose();
        wrist.parallelToGround();
    }

    private void three() {
        indexFinger.loose();
        middleFinger.loose();
        thumb.flex();
        ringFinger.loose();
        pinky.flex();
        forearm.loose();
        wrist.parallelToGround();
    }

    public void scissors() {
        indexFinger.loose();
        middleFinger.loose();
        thumb.flex();
        ringFinger.flex();
        pinky.flex();
        forearm.flex();
        wrist.perpendicularToGround();
    }

    public void mirrorScissors() {
        indexFinger.loose();
        middleFinger.loose();
        ringFinger.flex();
        pinky.flex();
        thumb.flex();
        forearm.loose();
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

    public void rpsDownCount() {
        indexFinger.flex();
        middleFinger.flex();
        ringFinger.flex();
        pinky.flex();
        thumb.flex();
        forearm.minorFlex();
        wrist.perpendicularToGround();
    }

    public void mirrorRock() {
        indexFinger.flex();
        middleFinger.flex();
        ringFinger.flex();
        pinky.flex();
        thumb.flex();
        forearm.loose();
        wrist.parallelToGround();
    }

    public void paper() {
        indexFinger.loose();
        middleFinger.loose();
        ringFinger.loose();
        pinky.loose();
        thumb.loose();
        forearm.flex();
        wrist.parallelToGround();
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
        middleFinger.setAngle(50);
        ringFinger.setAngle(50);
        pinky.setAngle(50);
        indexFinger.flex();
        thumb.flex();
        forearm.loose();
        wrist.parallelToGround();
    }

    public void moveToRPSReady() {
        indexFinger.flex();
        middleFinger.flex();
        ringFinger.flex();
        pinky.flex();
        thumb.flex();
        forearm.loose();
        wrist.perpendicularToGround();
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
        thumb.flex();
        wrist.parallelToGround();
        forearm.loose();
    }

    public void moveToSimonSaysReady() {
        loose();
    }

    public void thumbsUp() {
        indexFinger.flex();
        middleFinger.flex();
        ringFinger.flex();
        pinky.flex();
        thumb.loose();
        wrist.perpendicularToGround();
        forearm.flex();
    }

    public void shutdown() {
        pwmDriver.shutDown();
    }
}
