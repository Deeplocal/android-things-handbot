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
    private static final Integer SAMPLES_TO_START_GAME = 4;

    private HandController handController;

    private Game currentGame;

    private Map<String, Integer> monitoredActions = new HashMap<>();

    private STATES currentState = STATES.IDLE;

    private String lastMirroredAction;

    private int consecutiveMirroredActions;

    private List<String> GAME_START_ACTIONS = Lists.newArrayList("rock", "scissors");

    private LightRingControl lightRingControl;

    private SoundController soundController;

    private enum STATES {
        IDLE,
        ROCK_PAPER_SCISSORS,
        MATCHING,
        MIRROR;
    }

    public void init(HandController handController, LightRingControl lightRingControl, SoundController soundController) {
        this.handController = handController;
        this.lightRingControl = lightRingControl;
        this.soundController = soundController;
        this.currentState = STATES.MIRROR;
        this.lightRingControl.runSwirl(5);
    }

    public void run(String action) {
        if (currentState == STATES.MIRROR) {
            logAction(action);

            if (shouldStartGame()) {
                clearLoggedActions();
                startGame();
            } else if (shouldMirror()) {
                clearLoggedActions();
                runMirror(action);
            }
        } else if (currentState == STATES.ROCK_PAPER_SCISSORS) {
            runGame(action);
        } else if (currentState == STATES.MATCHING) {
            runGame(action);
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
            currentGame = new RockPaperScissors(handController, this, lightRingControl);
            currentGame.start();
//            lightRingControl.runPulse(1);
        } else if ("scissors".equals(lastMirroredAction)) {
            currentState = STATES.MATCHING;
            currentGame = new SimonSays(handController, this, soundController);
            currentGame.start();
        }
        consecutiveMirroredActions = 0;
    }

    private boolean shouldStartGame() {
        return consecutiveMirroredActions >= SAMPLES_TO_START_GAME &&
                GAME_START_ACTIONS.contains(lastMirroredAction);
    }

    private boolean shouldMirror() {
        return getSmoothedAction() != null;
    }

    private void runMirror(String action) {
        Log.i("MIRROR_STATE", "run: " + action);
        if (action.equals(lastMirroredAction)) {
            consecutiveMirroredActions++;
        } else {
            consecutiveMirroredActions = 0;
        }
        lastMirroredAction = action;
        handController.runMirror(action);
    }

    private void runGame(String action) {
        if (currentGame != null) {
            currentGame.run(action);
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

    private void logAction(String seenAction) {
        if (!monitoredActions.containsKey(seenAction)) {
            monitoredActions.put(seenAction, 0);
        }
        Integer oldValue = monitoredActions.get(seenAction);
        monitoredActions.put(seenAction, oldValue + 1);
    }

    private void clearLoggedActions() {
        monitoredActions = new HashMap();
    }
}
