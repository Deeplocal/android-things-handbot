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

    private static final String TAG = SimonSays.class.getSimpleName();

    private static final long MOVE_TO_READY_WAIT_TIME      = 1200;
    private static final long SHOW_SIGN_WAIT_TIME          = 900;
    private static final long END_GAME_WAIT_TIME           = 2000;
    private static final long MONITOR_FOR_SIGN_WAIT_TIME   = 2500;
    private static final long PAUSE_BETWEEN_SIGN_WAIT_TIME = 600;
    private static final long CHOOSE_SIGNS_WAIT_DELAY      = 3500;
    private static final float MIN_SIGN_CONFIDENCE         = 0.60f;

    private static final int MAX_ROUNDS                    = 5;

    private String[] ACTIONS = new String[] { Signs.ROCK, Signs.SCISSORS, Signs.SPIDERMAN, Signs.LOSER, Signs.THREE, Signs.ONE, Signs.OK };

    private Map<String, Integer> monitoredActions = new HashMap();

    private long timeToTransition = System.currentTimeMillis();

    private List<String> signsToShow = new LinkedList<>();

    private List<String> signsToMatch = new LinkedList<>();

    private final LightRingControl lightRingControl;

    private GameStateListener gameStateListener;

    private static final int DEFAULT_SIGNS = 3;

    private STATES currentState = STATES.IDLE;

    private SoundController soundController;

    private HandController handController;

    private String actionToLookup;

    private int correctSigns = 0;

    private int roundNumber = 0;

    private int showedSigns = 0;

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
        CHOOSE_SIGNS_WAIT,
        SHOW_SIGN,
        SHOW_SIGN_WAIT,
        PAUSE_BETWEEN_SIGN,
        PAUSE_BETWEEN_SIGN_WAIT,
        DETERMINE_IF_MORE_SIGNS_TO_SHOW,
        PAUSE_BEFORE_MONITORING,
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
        switch (currentState) {
            case IDLE:
                break;
            case INITIALIZE:
                roundNumber = 0;
                monitoredActions = new HashMap();
                currentState = STATES.MOVE_TO_READY;
                break;
            case MOVE_TO_READY:
                setTransitionTime(MOVE_TO_READY_WAIT_TIME);
                handController.moveToSimonSaysReady();
                currentState = STATES.MOVE_TO_READY_WAIT;
                break;
            case MOVE_TO_READY_WAIT:
                currentState = nextStateForWaitState(STATES.CHOOSE_SIGNS);
                break;
            case CHOOSE_SIGNS:
                showedSigns = 0;
                generateSigns();
                lightRingControl.runScorePulse(2, signsToShow.size(), 0);
                soundController.playSound(SoundController.SOUNDS.MIRROR);
                currentState = STATES.CHOOSE_SIGNS_WAIT;
                setTransitionTime(CHOOSE_SIGNS_WAIT_DELAY);
                break;
            case CHOOSE_SIGNS_WAIT:
                currentState = nextStateForWaitState(STATES.SHOW_SIGN);
                break;
            case SHOW_SIGN:
                setTransitionTime(SHOW_SIGN_WAIT_TIME);
                String sign = signsToShow.remove(0);
                Log.i(TAG, "sign: " + sign);
                showedSigns++;
                lightRingControl.showMatchingLights(showedSigns, 0);
                soundController.playSignSound(sign);
                handController.handleSimonSaysAction(sign);
                currentState = STATES.SHOW_SIGN_WAIT;
                break;
            case SHOW_SIGN_WAIT:
                currentState = nextStateForWaitState(STATES.DETERMINE_IF_MORE_SIGNS_TO_SHOW);
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
                currentState = signsToShow.size() > 0 ? STATES.SHOW_SIGN : STATES.PAUSE_BEFORE_MONITORING;
                if (currentState == STATES.PAUSE_BEFORE_MONITORING) {
                    lightRingControl.runScorePulse(2, signsToMatch.size(), 0);
                    soundController.playSound(SoundController.SOUNDS.MIRROR);
                    setTransitionTime(MONITOR_FOR_SIGN_WAIT_TIME);
                }
                break;
            case PAUSE_BEFORE_MONITORING:
                currentState = nextStateForWaitState(STATES.MONITOR_FOR_SIGN);
                if (currentState == STATES.MONITOR_FOR_SIGN) {
                    setTransitionTime(MONITOR_FOR_SIGN_WAIT_TIME);
                }
                break;
            case MONITOR_FOR_SIGN:
                actionToLookup = signsToMatch.get(0);
                currentState = nextStateForWaitState(STATES.DETERMINE_SIGN_CORRECT);
                long timeLeft = timeToTransition - System.currentTimeMillis();
                if (action.equals(actionToLookup) && results.get(0).getConfidence() > 0.9f && ((float)timeLeft < ((float)MONITOR_FOR_SIGN_WAIT_TIME) * 0.75f)) {
                    currentState = STATES.DETERMINE_SIGN_CORRECT;
                }
                logAction(action, results);
                break;
            case DETERMINE_SIGN_CORRECT:
                if (monitoredActions.containsKey(signsToMatch.get(0))) {
                    String matchedSign = signsToMatch.remove(0);
                    correctSigns++;
                    if (signsToMatch.size() > 0) {
                        currentState = STATES.PLAY_CORRECT_SIGN;
                        setTransitionTime(MONITOR_FOR_SIGN_WAIT_TIME);
                        soundController.playSignSound(matchedSign);
                    } else {
                        currentState = STATES.PREPARE_FOR_NEXT_ROUND;
                    }
                    lightRingControl.showMatchingLights(correctSigns, 0);
                    actionToLookup = null;
                } else {
                    currentState = STATES.PLAY_INCORRECT_SIGN;
                }
                monitoredActions = new HashMap();
                break;
            case PLAY_CORRECT_SIGN:
                currentState = STATES.MONITOR_FOR_SIGN;
                break;
            case PLAY_INCORRECT_SIGN:
                lightRingControl.showMatchingLights(correctSigns, 1);
                currentState = STATES.LOSS;
                break;
            case PREPARE_FOR_NEXT_ROUND:
                if (roundNumber >= MAX_ROUNDS) {
                    currentState = STATES.WIN;
                } else {
                    soundController.playSound(SoundController.SOUNDS.CORRECT);
                    roundNumber++;
                    currentState = STATES.MOVE_TO_READY;
                }
                break;
            case WIN:
                lightRingControl.runPulse(2, Color.GREEN);
                soundController.playSound(SoundController.SOUNDS.WIN);
                setTransitionTime(END_GAME_WAIT_TIME);
                handController.thumbsUp();
                currentState = STATES.WIN_WAIT;
                break;
            case WIN_WAIT:
                currentState = nextStateForWaitState(STATES.GAME_OVER);
                break;
            case LOSS:
                lightRingControl.runScorePulse(2, correctSigns, 1);
                setTransitionTime(END_GAME_WAIT_TIME);
                soundController.playSound(SoundController.SOUNDS.LOSS);
                currentState = STATES.LOSS_WAIT;
                break;
            case LOSS_WAIT:
                currentState = nextStateForWaitState(STATES.GAME_OVER);
                break;
            case GAME_OVER:
                handController.loose();
                if (gameStateListener != null) {
                    gameStateListener.gameFinished();
                }
                currentState = STATES.IDLE;
                break;
        }
    }

    private void generateSigns() {
        signsToMatch.clear();
        signsToShow.clear();
        correctSigns = 0;
        int lastIndex = ACTIONS.length + 1;
        for (int i = 0; i < (DEFAULT_SIGNS + roundNumber); i++) {
            int actionIndex = randomExcluded(0, ACTIONS.length - 1, lastIndex);
            String action = ACTIONS[actionIndex];
            lastIndex = actionIndex;
            signsToShow.add(action);
            signsToMatch.add(action);
        }
    }

    private int randomExcluded(int min, int max, int excluded) {
        int n = (int) Math.floor(Math.random() * (max-min) + min);
        if (n >= excluded) n++;
        return n;
    }

    @Override
    public void shutdown() {
        gameStateListener = null;
        handController = null;
    }

    @Override
    public void start() {
        lightRingControl.runSwirl(1, Color.GREEN);
        handController.moveToSimonSaysReady();
        soundController.playSound(SoundController.SOUNDS.START_GAME);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                currentState = STATES.INITIALIZE;
            }
        }, 2000);
    }

    @Override
    public void stop() {
        currentState = STATES.IDLE;
    }

    @Override
    public String getClassifierKey() {
        return actionToLookup == null ? "simon_says" : actionToLookup;
    }

    private void logAction(String seenAction, List<Classifier.Recognition> results) {
        if (results.size() > 0 && results.get(0).getConfidence() > MIN_SIGN_CONFIDENCE && !Signs.NEGATIVE.equals(seenAction)) {
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
