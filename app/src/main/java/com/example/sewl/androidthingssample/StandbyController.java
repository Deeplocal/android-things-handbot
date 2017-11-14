package com.example.sewl.androidthingssample;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
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

    private List<String> GAME_START_ACTIONS = Lists.newArrayList(Signs.ROCK, Signs.SCISSORS);

    private LightRingControl lightRingControl;

    private SoundController soundController;

    private Handler pulseHandler;

    private int seenActions = 0;

    private int consecutiveNegativeActions;

    public enum STATES {
        IDLE,
        ROCK_PAPER_SCISSORS,
        MATCHING,
        MIRROR,
        START_RUN_PULSE,
        PULSE
    }

    public void init(final HandController handController, LightRingControl lightRingControl, SoundController soundController) {
        this.handController = handController;
        this.lightRingControl = lightRingControl;
        this.soundController = soundController;
        this.currentState = STATES.MIRROR;
        this.pulseHandler = new Handler(Looper.getMainLooper());
        lightRingControl.runPulse(1, Color.BLUE);
    }

    public void run(String action, List<Classifier.Recognition> results) {
        Log.i("STATE", "standby: " + currentState);
        if (currentState == STATES.MIRROR) {
            logMirrorAction(action, results);

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
        } else if (currentState == STATES.START_RUN_PULSE) {
            lightRingControl.runPulse(1, Color.BLUE);
            runPulse();
            currentState = STATES.MIRROR;
        } else if (currentState == STATES.PULSE) {
            runPulseState(action, results);
        }
    }

    private void runPulseState(String action, List<Classifier.Recognition> results) {
        if (!Signs.NEGATIVE.equals(action) && results.get(0).getConfidence() > 0.7f) {
            currentState = STATES.MIRROR;
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

    public String getClassifierKey() {
        if (currentState == STATES.IDLE) {
            return null;
        } else if (currentState == STATES.MIRROR) {
            return "mirror";
        } else if (currentState == STATES.START_RUN_PULSE) {
            return "rps";
        } else if (currentState == STATES.PULSE) {
            return "rps";
        } else {
            return currentGame != null ? currentGame.getClassifierKey() : null;
        }
    }

    private void runPulse() {
        pulseHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                lightRingControl.runPulse(1, Color.BLUE);
                runPulse();
            }
        }, 3000);
    }

    private void startGame() {
        if (Signs.ROCK.equals(lastMirroredAction)) {
            currentState = STATES.ROCK_PAPER_SCISSORS;
            currentGame = new RockPaperScissors(handController, this, lightRingControl, soundController);
            currentGame.start();
        } else if (Signs.SCISSORS.equals(lastMirroredAction)) {
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
        if (action.equals(lastMirroredAction)) {
            consecutiveMirroredActions++;
        } else {
            consecutiveMirroredActions = 0;
        }
        lastMirroredAction = action;
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

    private void logMirrorAction(String seenAction, List<Classifier.Recognition> results) {
//        if (seenAction.equals(Signs.NEGATIVE) && results.get(0).getConfidence() >= 0.5f) {
//            consecutiveNegativeActions++;
//            if (consecutiveNegativeActions >= 20) {
//                currentState = STATES.START_RUN_PULSE;
//                clearLoggedActions();
//                consecutiveNegativeActions = 0;
//                return;
//            }
//        } else {
//            consecutiveNegativeActions = 0;
//            pulseHandler.removeCallbacksAndMessages(null);
//        }

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
