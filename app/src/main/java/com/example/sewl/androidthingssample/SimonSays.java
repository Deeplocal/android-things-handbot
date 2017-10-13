package com.example.sewl.androidthingssample;

import android.util.Log;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by mderrick on 10/12/17.
 */

public class SimonSays implements Game {

    private static final Integer SAMPLES_PER_THROW = 3;
    private static final int MAX_ROUNDS = 10;

    private GameStateListener gameStateListener;

    private long currentTime;

    private String[] ACTIONS = new String[] { "rock", "paper", "scissors", "ok", "spiderman", "three", "loser" };

    private static final int DEFAULT_SIGNS = 3;

    private HandController handController;

    private STATES currentState = STATES.IDLE;

    private long timeToTransition = System.currentTimeMillis();

    private static final long ANIMATION_WAIT_TIME = 3000;

    private List<String> signsToShow = new LinkedList<>();

    private List<String> signsToMatch = new LinkedList<>();

    private int roundNumber = 0;

    private Map<String, Integer> monitoredActions = new HashMap();

    public SimonSays(HandController handController, GameStateListener gameStateListener) {
        this.handController = handController;
        this.gameStateListener = gameStateListener;
    }

    private enum SOUNDS {
        WIN,
        LOSS,
        CORRECT,
        INCORRECT
    }

    private enum STATES {
        IDLE,
        INITIALIZE,
        MOVE_TO_READY,
        MOVE_TO_READY_WAIT,
        CHOOSE_SIGNS,
        SHOW_SIGN,
        SHOW_SIGN_WAIT,
        MONITOR_FOR_SIGN,
        DETERMINE_SIGN_CORRECT,
        PLAY_CORRECT_SIGN,
        PLAY_INCORRECT_SIGN,
        PREPARE_FOR_NEXT_ROUND,
        WIN,
        WIN_WAIT,
        LOSS,
        LOSS_WAIT,
        GAME_OVER
    }

    @Override
    public void run(String action) {
        Log.i("TOOK", "time: " + (System.currentTimeMillis() - currentTime));
        switch (currentState) {
            case IDLE:
                Log.i("STATE", "state: IDLE");
                break;
            case INITIALIZE:
                Log.i("STATE", "state: INITIALIZE");
                roundNumber = 0;
                monitoredActions = new HashMap();
                currentState = STATES.MOVE_TO_READY;
                break;
            case MOVE_TO_READY:
                Log.i("STATE", "state: MOVE_TO_READY: " + roundNumber);
                setTransitionTime();
                handController.moveToRPSReady();
                currentState = STATES.MOVE_TO_READY_WAIT;
                break;
            case MOVE_TO_READY_WAIT:
                Log.i("STATE", "state: MOVE_TO_READY_WAIT");
                currentState = nextStateForWaitState(STATES.CHOOSE_SIGNS);
                break;
            case CHOOSE_SIGNS:
                Log.i("STATE", "state: CHOOSE_SIGNS");
                generateSigns();
                currentState = STATES.SHOW_SIGN;
                break;
            case SHOW_SIGN:
                Log.i("STATE", "state: SHOW_SIGN");
                setTransitionTime();
                String sign = signsToShow.remove(0);
                handController.handleAction(sign);
                currentState = STATES.SHOW_SIGN_WAIT;
                break;
            case SHOW_SIGN_WAIT:
                Log.i("STATE", "state: SHOW_SIGN_WAIT");
                currentState = nextStateForWaitState(STATES.MONITOR_FOR_SIGN);
                break;
            case MONITOR_FOR_SIGN:
                Log.i("STATE", "state: MONITOR_FOR_SIGN");
                logAction(action);
                if (getUserThrow() != null) {
                    currentState = STATES.DETERMINE_SIGN_CORRECT;
                }
                break;
            case DETERMINE_SIGN_CORRECT:
                Log.i("STATE", "state: DETERMINE_SIGN_CORRECT");
                String userThrow = getUserThrow();
                if (userThrow.equals(signsToMatch.get(0))) {
                    signsToMatch.remove(0);
                    if (signsToMatch.size() > 0) {
                        currentState = STATES.PLAY_CORRECT_SIGN;
                    } else {
                        currentState = STATES.PREPARE_FOR_NEXT_ROUND;
                    }
                } else {
                    currentState = STATES.PLAY_INCORRECT_SIGN;
                }
                break;
            case PLAY_CORRECT_SIGN:
                Log.i("STATE", "state: PLAY_CORRECT_SIGN");
                playSound(SOUNDS.CORRECT);
                currentState = STATES.MONITOR_FOR_SIGN;
                break;
            case PLAY_INCORRECT_SIGN:
                Log.i("STATE", "state: PLAY_INCORRECT_SIGN");
                playSound(SOUNDS.INCORRECT);
                currentState = STATES.LOSS;
                break;
            case PREPARE_FOR_NEXT_ROUND:
                Log.i("STATE", "state: PREPARE_FOR_NEXT_ROUND");
                if (roundNumber >= MAX_ROUNDS) {
                    currentState = STATES.WIN;
                } else {
                    playSound(SOUNDS.CORRECT);
                    monitoredActions = new HashMap();
                    roundNumber++;
                    currentState = STATES.MOVE_TO_READY;
                }
                break;
            case WIN:
                Log.i("STATE", "state: WIN");
                playSound(SOUNDS.WIN);
                setTransitionTime();
                handController.thumbsUp();
                currentState = STATES.WIN_WAIT;
                break;
            case WIN_WAIT:
                Log.i("STATE", "state: WIN_WAIT");
                currentState = nextStateForWaitState(STATES.GAME_OVER);
                break;
            case LOSS:
                Log.i("STATE", "state: LOSS");
                handController.thumbsDown();
                setTransitionTime();
                playSound(SOUNDS.LOSS);
                currentState = STATES.LOSS_WAIT;
                break;
            case LOSS_WAIT:
                Log.i("STATE", "state: LOSS_WAIT");
                currentState = nextStateForWaitState(STATES.GAME_OVER);
                break;
            case GAME_OVER:
                if (gameStateListener != null) {
                    gameStateListener.gameFinished();
                }
                currentState = STATES.IDLE;
                break;
        }
        currentTime = System.currentTimeMillis();
    }

    private void playSound(SOUNDS sound) {
        // TODO: Play a sound here
    }

    private void generateSigns() {
        signsToMatch.clear();
        signsToShow.clear();
        for (int i = 0; i < (DEFAULT_SIGNS + roundNumber); i++) {
            String action = ACTIONS[(int) (Math.random() * ACTIONS.length)];
            signsToShow.add(action);
            signsToMatch.add(action);
        }
    }

    @Override
    public void shutdown() {
        gameStateListener = null;
        handController = null;
    }

    @Override
    public void start() {
        currentState = STATES.INITIALIZE;
    }

    @Override
    public void stop() {
        currentState = STATES.IDLE;
    }

    private String getUserThrow() {
        for (String action : ACTIONS) {
            if (monitoredActions.containsKey(action) && monitoredActions.get(action) >= SAMPLES_PER_THROW) {
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

    private void setTransitionTime() {
        timeToTransition = System.currentTimeMillis() + ANIMATION_WAIT_TIME;
    }

    private STATES nextStateForWaitState(STATES nextState) {
        if (System.currentTimeMillis() >= timeToTransition) {
            return nextState;
        } else {
            return currentState;
        }
    }
}
