package com.example.sewl.androidthingssample;

import android.util.Log;

import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mderrick on 10/13/17.
 */

public class StandbyController implements GameStateListener {

    private static final Integer SAMPLES_PER_ACTION = 2;
    private static final Integer SAMPLES_TO_START_GAME = 8;

    private HandController handController;

    private Game currentGame;

    private Map<String, Integer> monitoredActions = new HashMap<>();

    private STATES currentState = STATES.IDLE;

    private String lastMirroredAction;

    private int consecutiveMirroredActions;

    private List<String> GAME_START_ACTIONS = Lists.newArrayList("rock", "scissors");

    private LightRingControl lightRingControl;

    private SoundController soundController;

    private int seenActions = 0;

    private enum STATES {
        IDLE,
        ROCK_PAPER_SCISSORS,
        MATCHING,
        MIRROR;

    }

    public void init(final HandController handController, LightRingControl lightRingControl, SoundController soundController) {
        this.handController = handController;
        this.lightRingControl = lightRingControl;
        this.soundController = soundController;
        this.currentState = STATES.MIRROR;
    }

    public void run(String action, List<Classifier.Recognition> results) {
        if (currentState == STATES.MIRROR) {
            logAction(action, results);

            if (shouldStartGame()) {
                clearLoggedActions();
                startGame();
            } else if (shouldMirror(results)) {
                clearLoggedActions();
                runMirror(action);
            }
        } else if (currentState == STATES.ROCK_PAPER_SCISSORS) {
            runGame(action, results);
        } else if (currentState == STATES.MATCHING) {
            runGame(action, results);
        }
    }

    @Override
    public void gameFinished() {
        if (currentGame != null) {
            currentGame.shutdown();
            currentGame = null;
        }
        currentState = STATES.MIRROR;
    }

    private void startGame() {
        if ("rock".equals(lastMirroredAction)) {
            currentState = STATES.ROCK_PAPER_SCISSORS;
//            currentGame = new RockPaperScissors(handController, this, lightRingControl);
            currentGame = new SimonSays(handController, this, soundController, lightRingControl);
            currentGame.start();
        } else if ("paper".equals(lastMirroredAction)) {
            currentState = STATES.MATCHING;
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
        return results.get(0).getConfidence() > 0.92f || getSmoothedAction() != null;
    }

    private void runMirror(String action) {
        Log.i("MIRROR_STATE", "run: " + action);
        if (action.equals(lastMirroredAction)) {
            consecutiveMirroredActions++;
        } else {
            consecutiveMirroredActions = 0;
        }
        lastMirroredAction = action;
        // TODO: Re-enable
        handController.runMirror(action);
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

    private void logAction(String seenAction, List<Classifier.Recognition> results) {
        if (seenActions >= 25) {
            clearLoggedActions();
        }
        seenActions++;

        if (results.get(0).getConfidence() < 0.85f) {
            return;
        }

        if (!monitoredActions.containsKey(seenAction)) {
            monitoredActions.put(   seenAction, 0);
        }

        Integer oldValue = monitoredActions.get(seenAction);
        monitoredActions.put(seenAction, oldValue + 1);
    }

    private void clearLoggedActions() {
        seenActions = 0;
        monitoredActions = new HashMap();
    }
}
