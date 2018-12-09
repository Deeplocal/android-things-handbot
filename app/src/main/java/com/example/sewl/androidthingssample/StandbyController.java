package com.example.sewl.androidthingssample;

import android.graphics.Color;

import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mderrick on 10/13/17.
 */

public class StandbyController implements GameStateListener {

    private static final Integer SAMPLES_PER_ACTION     = 2;
    private static final Integer SAMPLES_TO_START_GAME  = 4;
    public static final float MINIMUM_MIRROR_CONFIDENCE = 0.90f;

    private HandController handController;

    private Game currentGame;

    private Map<String, Integer> monitoredActions = new HashMap<>();

    private States currentState = States.IDLE;

    private String lastMirroredAction = "##";

    private int consecutiveMirroredActions;

    private List<String> GAME_START_ACTIONS = Lists.newArrayList(Signs.ROCK, Signs.SCISSORS);

    private LightRingControl lightRingControl;

    private SoundController soundController;

    private int seenActions = 0;

    private int consecutiveCoveredResults = 0;

    private int consecutiveNegatives;

    public enum States {
        IDLE,
        ROCK_PAPER_SCISSORS,
        MATCHING,
        MIRROR
    }

    public void init(final HandController handController, LightRingControl lightRingControl, SoundController soundController) {
        this.handController = handController;
        this.lightRingControl = lightRingControl;
        this.soundController = soundController;
        this.currentState = States.MIRROR;
        if (lightRingControl!=null) {
            lightRingControl.runPulse(1, Color.BLUE);
        }
    }

    public void run(String action, List<Classifier.Recognition> results) {
        if (currentState == States.MIRROR) {
            logMirrorAction(action, results);
            logConsecutiveNegatives(action);
            logConsecutiveCovered(action);

            if (shouldStartGame()) {
                consecutiveNegatives = 0;
                clearLoggedActions();
                startGame();
            } else if (consecutiveCoveredResults >= 7 ) {
                consecutiveCoveredResults = 0;
                runBackOffAnimation();
            } else if (consecutiveNegatives >= 50) {
                consecutiveNegatives = 0;
                handController.one();
            } else if (shouldMirror(results)) {
                clearLoggedActions();
                runMirror(action);
            }

        } else if (currentState == States.ROCK_PAPER_SCISSORS) {
            runGame(action, results);
        } else if (currentState == States.MATCHING) {
            runGame(action, results);
        }
    }

    public void reset() {
        currentState = States.MIRROR;
        consecutiveCoveredResults = 0;
        consecutiveNegatives = 0;
        clearLoggedActions();
        handController.loose();
        lightRingControl.setColor(Color.BLACK);
    }

    private void runBackOffAnimation() {
        if (lightRingControl!=null) {
            lightRingControl.flash(1, Color.RED);
        }
        soundController.playSound(SoundController.Sounds.TIE);
    }

    private void logConsecutiveNegatives(String action) {
        if (Signs.NEGATIVE.equals(action)) {
            consecutiveNegatives++;
        } else {
            consecutiveNegatives = 0;
        }
    }

    private void logConsecutiveCovered(String action) {
        if (Signs.COVERED.equals(action)) {
            consecutiveCoveredResults++;
        } else {
            consecutiveCoveredResults = 0;
        }
    }

    @Override
    public void gameFinished() {
        if (currentGame != null) {
            currentGame.shutdown();
            currentGame = null;
        }
        currentState = States.MIRROR;
    }

    public String getClassifierKey() {
        if (currentState == States.IDLE) {
            return null;
        } else if (currentState == States.MIRROR) {
            return "mirror";
        } else {
            return currentGame != null ? currentGame.getClassifierKey() : null;
        }
    }

    private void startGame() {

        if (Signs.ROCK.equals(lastMirroredAction)) {
            currentState = States.ROCK_PAPER_SCISSORS;
            currentGame = new RockPaperScissors(handController, this, lightRingControl, soundController);
            currentGame.start();
        } else if (Signs.SCISSORS.equals(lastMirroredAction)) {
            currentState = States.MATCHING;
            currentGame = new SimonSays(handController, this, soundController, lightRingControl);
            currentGame.start();
        }
        consecutiveMirroredActions = 0;
    }

    private boolean shouldStartGame() {
        return consecutiveMirroredActions >= SAMPLES_TO_START_GAME &&
                GAME_START_ACTIONS.contains(lastMirroredAction);
    }

    private boolean shouldMirror(List<Classifier.Recognition> results) {
        if (Signs.NEGATIVE.equals(results.get(0))) {
            return false;
        }
        return results.get(0).getConfidence() > MINIMUM_MIRROR_CONFIDENCE || getSmoothedAction() != null;
    }

    private void runMirror(String action) {
        if (!(action.equals(Signs.NEGATIVE)) ||(action.equals(Signs.COVERED))){
            if (action.equals(lastMirroredAction)) {
                consecutiveMirroredActions++;
            } else {
                consecutiveMirroredActions = 0;
            }
            lastMirroredAction = action;
            handController.runMirror(action);
        }
    }

    private void runGame(String action, List<Classifier.Recognition> results) {
        if (currentGame != null) {
            currentGame.run(action, results);
        }
    }

    private String getSmoothedAction() {
        for (String action : monitoredActions.keySet()) {
            if (monitoredActions.get(action) >= SAMPLES_PER_ACTION) {
                return action;
            }
        }
        return null;
    }

    private void logMirrorAction(String seenAction, List<Classifier.Recognition> results) {
        if (seenActions >= 30) {
            clearLoggedActions();
        }
        seenActions++;

        if (results.get(0).getConfidence() < 0.50f) {
            return;
        }

        if (!monitoredActions.containsKey(seenAction)) {
            monitoredActions.put(seenAction, 0);
        }

        Integer oldValue = monitoredActions.get(seenAction);
        monitoredActions.put(seenAction, oldValue + 1);
    }

    private void clearLoggedActions() {
        seenActions = 0;
        monitoredActions = new HashMap();
    }
}
