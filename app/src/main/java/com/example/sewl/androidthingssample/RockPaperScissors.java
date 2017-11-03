package com.example.sewl.androidthingssample;

import android.graphics.Color;
import android.os.Handler;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mderrick on 10/11/17.
 */

public class RockPaperScissors implements Game {

    public static final int WIN_SAMPLES_NEEDED         = 2;
    private static final long THROW_WAIT_TIME          = 600;
    private static final long WAIT_FOR_NEW_ROUND_DELAY = 800;
    private static final long START_ROUND_TIME         = 1500;
    public static final int MONITOR_TIME               = 2000;
    private static final int ORANGE                    = 0xFFA500;

    private GameStateListener gameStateListener;

    private SoundController soundController;

    private HandController handController;

    private LightRingControl lightRingControl;

    private Map<String, Integer> monitoredActions = new HashMap<>();

    private int totalRecordedActions;

    private int roundLosses;

    private int roundWins;

    private long timeToTransition = System.currentTimeMillis();

    private String[] ACTIONS = new String[] { "rock", "paper", "scissors" };

    private String thrownAction;

    private Handler handler;

    private static final long ANIMATION_WAIT_TIME = 3000;

    private Thread rpsThread;

    public RockPaperScissors(HandController handController, GameStateListener gameStateListener,
                             LightRingControl lightRingControl, SoundController soundController) {
        this.handController = handController;
        this.gameStateListener = gameStateListener;
        this.lightRingControl = lightRingControl;
        this.soundController = soundController;
        this.handler = new Handler();
    }

    private enum STATES {
        IDLE,
        INITIATE,
        INITIATE_WAIT,
        COUNTDOWN,
        COUNTDOWN_WAIT,
        THROW,
        THROW_WAIT,
        MONITOR,
        DETERMINE_ROUND_WINNER,
        WIN,
        LOSS,
        WAIT_FOR_NEW_ROUND,
        WAIT_FOR_NEW_GAME,
        END_GAME,
        END_GAME_WAIT,
        GAME_OVER
    }

    private enum GAME_RESULTS {
        WIN,
        LOSS,
        TIE
    }

    private STATES currentState = STATES.IDLE;

    @Override
    public void shutdown() {
        handController = null;
        gameStateListener = null;
    }

    @Override
    public void start() {
        lightRingControl.runSwirl(2, Color.BLUE);
        handController.loose();
        soundController.playSound(SoundController.SOUNDS.START_GAME);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                currentState = STATES.INITIATE;
            }
        }, 3000);
    }

    @Override
    public void stop() {
        currentState = STATES.IDLE;
    }

    @Override
    public void run(String seenAction, List<Classifier.Recognition> results) {
        Log.i("STATE", "state: " + currentState);
        if (currentState == STATES.IDLE) {
            resetGame();
        } else if (currentState == STATES.INITIATE) {
            resetRound();
            handController.moveToRPSReady();
            setTransitionTime(START_ROUND_TIME);
            currentState = STATES.INITIATE_WAIT;
        } else if (currentState == STATES.INITIATE_WAIT) {
            currentState = nextStateForWaitState(STATES.COUNTDOWN);
        } else if (currentState == STATES.COUNTDOWN) {
            lightRingControl.setRPSScore(roundWins, roundLosses);
            currentState = STATES.THROW_WAIT;
            thrownAction = ACTIONS[(int)(Math.random() * ACTIONS.length)];
            setTransitionTime(2200);
            runRPSCountdown();
        } else if (currentState == STATES.THROW_WAIT) {
            currentState = nextStateForWaitState(STATES.MONITOR);
            if (currentState == STATES.MONITOR) {
                setTransitionTime(MONITOR_TIME);
            }
        } else if (currentState == STATES.MONITOR) {
            logAction(seenAction);
            currentState = nextStateForWaitState(STATES.DETERMINE_ROUND_WINNER);
            totalRecordedActions++;
        } else if (currentState == STATES.DETERMINE_ROUND_WINNER) {
            String userThrow = getUserThrow();
            GAME_RESULTS gameResults = getGameResults(userThrow);
            Log.i("STATE", "vs: " + thrownAction + " vs " + userThrow);
            if (gameResults == GAME_RESULTS.WIN) {
                roundWins++;
            } else if (gameResults == GAME_RESULTS.LOSS) {
                roundLosses++;
            }

            if (gameOver()) {
                currentState = roundWins > roundLosses ? STATES.WIN : STATES.LOSS;
            } else {
                setTransitionTime(WAIT_FOR_NEW_ROUND_DELAY);
                currentState = STATES.WAIT_FOR_NEW_ROUND;
                if (gameResults == GAME_RESULTS.TIE) {
                    lightRingControl.flash(1, ORANGE);
                    soundController.playSound(SoundController.SOUNDS.TIE);
                } else if (gameResults == GAME_RESULTS.WIN) {
                    soundController.playSound(SoundController.SOUNDS.ROUND_WIN);
                } else {
                    soundController.playSound(SoundController.SOUNDS.ROUND_LOSS);
                }
            }
            lightRingControl.setRPSScore(roundWins, roundLosses);
        } else if (currentState == STATES.WIN) {
            soundController.playSound(SoundController.SOUNDS.WIN);
            lightRingControl.runSwirl(3, Color.GREEN);
            setTransitionTime(ANIMATION_WAIT_TIME);
            currentState = STATES.WAIT_FOR_NEW_GAME;
        } else if (currentState == STATES.LOSS) {
            soundController.playSound(SoundController.SOUNDS.LOSS);
            lightRingControl.runSwirl(3, Color.RED);
            setTransitionTime(ANIMATION_WAIT_TIME);
            currentState = STATES.WAIT_FOR_NEW_GAME;
        } else if (currentState == STATES.WAIT_FOR_NEW_ROUND) {
            currentState = nextStateForWaitState(STATES.INITIATE);
        } else if (currentState == STATES.WAIT_FOR_NEW_GAME) {
            currentState = nextStateForWaitState(STATES.END_GAME);
        } else if (currentState == STATES.END_GAME) {
            handController.loose();
            setTransitionTime(ANIMATION_WAIT_TIME);
            currentState = STATES.END_GAME_WAIT;
        } else if (currentState == STATES.END_GAME_WAIT) {
            currentState = nextStateForWaitState(STATES.GAME_OVER);
        } else if (currentState == STATES.GAME_OVER) {
            lightRingControl.setRPSScore(0, 0);
            if (gameStateListener != null) {
                gameStateListener.gameFinished();
            }
            currentState = STATES.IDLE;
        }
    }

    private void runRPSCountdown() {
        final int sleepTime = 350;
        final int pauseTime = 200;
        final int afterSoundTime = 100;
        rpsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                soundController.playSound(SoundController.SOUNDS.RPS_BING);
                sleep(afterSoundTime);
                handController.rpsDownCount();
                sleep(pauseTime);
                handController.moveToRPSReady();
                sleep(sleepTime);
                soundController.playSound(SoundController.SOUNDS.RPS_BING);
                sleep(afterSoundTime);
                handController.rpsDownCount();
                sleep(pauseTime);
                handController.moveToRPSReady();
                sleep(sleepTime);
                soundController.playSound(SoundController.SOUNDS.RPS_BING);
                sleep(afterSoundTime);
                handController.rpsDownCount();
                sleep(pauseTime);
                handController.moveToRPSReady();
                sleep(sleepTime);
                soundController.playSound(SoundController.SOUNDS.RPS_BONG);
                sleep(afterSoundTime);
                handController.handleRPSAction(thrownAction);

                if (rpsThread != null) {
                    rpsThread.interrupt();
                }
            }

            private void sleep(long time) {
                try {
                    Thread.sleep(time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        rpsThread.start();
    }

    @Override
    public String getClassifierKey() {
        return "rps";
    }

    private boolean gameOver() {
        return roundLosses == 2 || roundWins == 2 ||
                (roundWins + roundLosses) >= 3;
    }

    private GAME_RESULTS getGameResults(String seenAction) {
        if (seenAction == null) {
            return GAME_RESULTS.TIE;
        }

        if (seenAction.equals("rock")) {
            if (thrownAction.equals("rock")) {
                return GAME_RESULTS.TIE;
            } else if (thrownAction.equals("paper")) {
                return GAME_RESULTS.LOSS;
            } else {
                return GAME_RESULTS.WIN;
            }
        } else if (seenAction.equals("paper")) {
            if (thrownAction.equals("rock")) {
                return GAME_RESULTS.WIN;
            } else if (thrownAction.equals("paper")) {
                return GAME_RESULTS.TIE;
            } else {
                return GAME_RESULTS.LOSS;
            }
        } else if (seenAction.equals("scissors")) {
            if (thrownAction.equals("rock")) {
                return GAME_RESULTS.LOSS;
            } else if (thrownAction.equals("paper")) {
                return GAME_RESULTS.WIN;
            } else {
                return GAME_RESULTS.TIE;
            }
        }
        return GAME_RESULTS.TIE;
    }

    private String getUserThrow() {
        int mostSamples = 0;
        String mostSampledAction = null;
        for (String action : ACTIONS) {
            boolean actionOccured = monitoredActions.containsKey(action);
            if (actionOccured && monitoredActions.get(action) > mostSamples) {
                mostSampledAction = action;
                mostSamples = monitoredActions.get(action);
            }
        }
        return mostSampledAction;
    }

    private void resetGame() {
        roundLosses = 0;
        roundWins = 0;
        lightRingControl.setRPSScore(0, 0);
    }

    private void setTransitionTime(long delay) {
        timeToTransition = System.currentTimeMillis() + delay;
    }

    private void resetRound() {
        totalRecordedActions = 0;
        monitoredActions = new HashMap();
    }

    private STATES nextStateForWaitState(STATES nextState) {
        if (System.currentTimeMillis() >= timeToTransition) {
            return nextState;
        } else {
            return currentState;
        }
    }

    private void logAction(String seenAction) {
        if ("negative".equals(seenAction)) {
            return;
        }

        if (!monitoredActions.containsKey(seenAction)) {
            monitoredActions.put(seenAction, 0);
        }
        Integer oldValue = monitoredActions.get(seenAction);
        monitoredActions.put(seenAction, oldValue + 1);
    }
}
