package com.example.sewl.androidthingssample;

import android.graphics.Color;
import android.os.Handler;
import android.util.Log;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by mderrick on 10/12/17.
 */

public class SimonSays implements Game {

    private static final Integer SAMPLES_PER_THROW         = 3;
    private static final int MAX_ROUNDS                    = 10;
    private static final long MOVE_TO_READY_WAIT_TIME      = 500;
    private static final long SHOW_SIGN_WAIT_TIME          = 2000;
    private static final long POINT_WAIT_TIME              = 2000;
    private static final long MONITOR_FOR_SIGN_WAIT_TIME   = 2500;
    private static final long PAUSE_BETWEEN_SIGN_WAIT_TIME = 300;
    private static final int SIGN_DURATION                 = 2;

    private final LightRingControl lightRingControl;

    private GameStateListener gameStateListener;

    private long currentTime;

//    private String[] ACTIONS = new String[] { "rock", "paper", "scissors", "ok", "spiderman", "three", "loser" };
    private String[] ACTIONS = new String[] { "rock", "scissors" };

    private static final int DEFAULT_SIGNS = 3;

    private HandController handController;

    private STATES currentState = STATES.IDLE;

    private long timeToTransition = System.currentTimeMillis();

    private List<String> signsToShow = new LinkedList<>();

    private List<String> signsToMatch = new LinkedList<>();

    private int roundNumber = 0;

    private SoundController soundController;

    private Map<String, Integer> monitoredActions = new HashMap();

    public SimonSays(HandController handController, GameStateListener gameStateListener, SoundController soundController, LightRingControl lightRingControl) {
        this.handController = handController;
        this.gameStateListener = gameStateListener;
        this.lightRingControl = lightRingControl;
        this.soundController = soundController;
    }

    private enum STATES {
        IDLE,
        INITIALIZE,
        MOVE_TO_READY,
        MOVE_TO_READY_WAIT,
        CHOOSE_SIGNS,
        SHOW_SIGN,
        SHOW_SIGN_WAIT,
        PAUSE_BETWEEN_SIGN,
        PAUSE_BETWEEN_SIGN_WAIT,
        DETERMINE_IF_MORE_SIGNS_TO_SHOW,
        POINT,
        POINT_WAIT,
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
    public void run(String action, List<Classifier.Recognition> results) {
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
                setTransitionTime(MOVE_TO_READY_WAIT_TIME);
                handController.moveToSimonSaysReady();
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
                setTransitionTime(SHOW_SIGN_WAIT_TIME);
                lightRingControl.runSwirl(1, Color.BLUE, SIGN_DURATION);
                String sign = signsToShow.remove(0);
                Log.i("STATE", "state: SHOW_SIGN: " + sign);
                handController.handleSimonSaysAction(sign);
                currentState = STATES.SHOW_SIGN_WAIT;
                break;
            case SHOW_SIGN_WAIT:
                Log.i("STATE", "state: SHOW_SIGN_WAIT");
                currentState = nextStateForWaitState(STATES.PAUSE_BETWEEN_SIGN);
                break;
            case PAUSE_BETWEEN_SIGN:
                setTransitionTime(PAUSE_BETWEEN_SIGN_WAIT_TIME);
                handController.moveToSimonSaysReady();
                currentState = STATES.PAUSE_BETWEEN_SIGN_WAIT;
                break;
            case PAUSE_BETWEEN_SIGN_WAIT:
                currentState = nextStateForWaitState(STATES.DETERMINE_IF_MORE_SIGNS_TO_SHOW);
                break;
            case DETERMINE_IF_MORE_SIGNS_TO_SHOW:
                currentState = signsToShow.size() > 0 ? STATES.SHOW_SIGN : STATES.POINT;
                break;
            case POINT:
                // TODO: point when user is up
//                handController.point();
                setTransitionTime(POINT_WAIT_TIME);
                currentState = STATES.POINT_WAIT;
                break;
            case POINT_WAIT:
                currentState = nextStateForWaitState(STATES.MONITOR_FOR_SIGN);
                if (currentState == STATES.MONITOR_FOR_SIGN) {
                    setTransitionTime(MONITOR_FOR_SIGN_WAIT_TIME);
                    lightRingControl.runSwirl(1, SIGN_DURATION);
                }
            case MONITOR_FOR_SIGN:
                Log.i("STATE", "state: MONITOR_FOR_SIGN");
                logAction(action, results);
                currentState = nextStateForWaitState(STATES.DETERMINE_SIGN_CORRECT);
                break;
            case DETERMINE_SIGN_CORRECT:
                Log.i("STATE", "state: DETERMINE_SIGN_CORRECT");
                if (monitoredActions.containsKey(signsToMatch.get(0))) {
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
                soundController.playSound(SoundController.SOUNDS.CORRECT);
                lightRingControl.runSwirl(1, SIGN_DURATION);
                currentState = STATES.MONITOR_FOR_SIGN;
                break;
            case PLAY_INCORRECT_SIGN:
                Log.i("STATE", "state: PLAY_INCORRECT_SIGN");
                soundController.playSound(SoundController.SOUNDS.INCORRECT);
                currentState = STATES.LOSS;
                break;
            case PREPARE_FOR_NEXT_ROUND:
                Log.i("STATE", "state: PREPARE_FOR_NEXT_ROUND");
                if (roundNumber >= MAX_ROUNDS) {
                    currentState = STATES.WIN;
                } else {
                    soundController.playSound(SoundController.SOUNDS.CORRECT);
                    monitoredActions = new HashMap();
                    roundNumber++;
                    currentState = STATES.MOVE_TO_READY;
                }
                break;
            case WIN:
                Log.i("STATE", "state: WIN");
                lightRingControl.runPulse(3, Color.BLUE);
                soundController.playSound(SoundController.SOUNDS.WIN);
                setTransitionTime(SHOW_SIGN_WAIT_TIME);
                handController.thumbsUp();
                currentState = STATES.WIN_WAIT;
                break;
            case WIN_WAIT:
                Log.i("STATE", "state: WIN_WAIT");
                currentState = nextStateForWaitState(STATES.GAME_OVER);
                break;
            case LOSS:
                Log.i("STATE", "state: LOSS");
                lightRingControl.runPulse(2, Color.RED);
                setTransitionTime(SHOW_SIGN_WAIT_TIME);
                soundController.playSound(SoundController.SOUNDS.LOSS);
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
        lightRingControl.runSwirl(2, Color.GREEN);
        handController.moveToSimonSaysReady();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                currentState = STATES.INITIALIZE;
            }
        }, 3000);
    }

    @Override
    public void stop() {
        currentState = STATES.IDLE;
    }

    private void logAction(String seenAction, List<Classifier.Recognition> results) {
        if (results.size() > 0 && results.get(0).getConfidence() > 0.85f) {
            if (!monitoredActions.containsKey(seenAction)) {
                monitoredActions.put(seenAction, 0);
            }
            Integer oldValue = monitoredActions.get(seenAction);
            monitoredActions.put(seenAction, oldValue + 1);
        }
    }

    private void setTransitionTime(long waitTime) {
        timeToTransition = System.currentTimeMillis() + waitTime;
    }

    private STATES nextStateForWaitState(STATES nextState) {
        if (System.currentTimeMillis() >= timeToTransition) {
            return nextState;
        } else {
            return currentState;
        }
    }
}
