package com.example.sewl.androidthingssample;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mderrick on 10/11/17.
 */

public class RockPaperScissors implements Game {

    public static final int WIN_SAMPLES_NEEDED = 3;
    private static final long THROW_WAIT_TIME = 600;
    private static final long WAIT_FOR_NEW_ROUND_DELAY = 800;
    private static final long START_ROUND_TIME = 1500;

    private GameStateListener gameStateListener;

    private HandController handController;

    private LightRingControl lightRingControl;

    private Map<String, Integer> monitoredActions = new HashMap<>();

    private int totalRecordedActions;

    private int roundLosses;

    private int roundWins;

    private long timeToTransition = System.currentTimeMillis();

    private String[] ACTIONS = new String[] { "rock", "paper", "scissors" };

    private String thrownAction;

    private static final long ANIMATION_WAIT_TIME = 3000;

    public RockPaperScissors(HandController handController, GameStateListener gameStateListener, LightRingControl lightRingControl) {
        this.handController = handController;
        this.gameStateListener = gameStateListener;
        this.lightRingControl = lightRingControl;
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
        lightRingControl.runSwirl(4);
        handController.loose();
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
    public void run(String seenAction) {
        if (currentState == STATES.IDLE) {
            resetGame();
            Log.i("STATE", "IDLE");
        } else if (currentState == STATES.INITIATE) {
            Log.i("STATE", "STATES.INITIATE");
            resetRound();
            handController.loose();
            setTransitionTime(START_ROUND_TIME);
            currentState = STATES.INITIATE_WAIT;
        } else if (currentState == STATES.INITIATE_WAIT) {
            Log.i("STATE", "STATES.INITIATE_WAIT");
            currentState = nextStateForWaitState(STATES.COUNTDOWN);
        } else if (currentState == STATES.COUNTDOWN) {
            Log.i("STATE", "STATES.COUNTDOWN");
            currentState = STATES.COUNTDOWN_WAIT;
            setTransitionTime(2400);
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    handController.pinky.setAngle(120);
                }
            }, 300);
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    handController.loose();
                }
            }, 600);
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    handController.pinky.setAngle(120);
                }
            }, 1200);
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    handController.loose();
                }
            }, 1500);
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    handController.pinky.setAngle(120);
                }
            }, 2100);
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    handController.loose();
                }
            }, 2400);
        } else if (currentState == STATES.COUNTDOWN_WAIT) {
            Log.i("STATE", "STATES.COUNTDOWN_WAIT");
            currentState = nextStateForWaitState(STATES.THROW);
        } else if (currentState == STATES.THROW) {
            Log.i("STATE", "STATES.THROW");
            setTransitionTime(THROW_WAIT_TIME);
            thrownAction = ACTIONS[(int)(Math.random() * ACTIONS.length)];
            handController.handleAction(thrownAction);
            currentState = STATES.THROW_WAIT;
        } else if (currentState == STATES.THROW_WAIT) {
            Log.i("STATE", "STATES.THROW_WAIT");
            currentState = nextStateForWaitState(STATES.MONITOR);
        } else if (currentState == STATES.MONITOR) {
            Log.i("STATE", "STATES.MONITOR: " + seenAction);
            logAction(seenAction);
            if (getUserThrow() != null) {
                currentState = STATES.DETERMINE_ROUND_WINNER;
            } else if (totalRecordedActions >= 30) {
                currentState = STATES.LOSS;
            }
            totalRecordedActions++;
        } else if (currentState == STATES.DETERMINE_ROUND_WINNER) {
            Log.i("STATE", "STATES.DETERMINE_ROUND_WINNER");
            String userThrow = getUserThrow();
            GAME_RESULTS gameResults = getGameResults(userThrow);
            if (gameResults == GAME_RESULTS.WIN) {
                Log.i("STATE", "win: " + thrownAction + " vs " + userThrow);
                roundWins++;
            } else if (gameResults == GAME_RESULTS.LOSS) {
                Log.i("STATE", "loss: " + thrownAction + " vs " + userThrow);
                roundLosses++;
            } else {
                Log.i("STATE", "tie: " + thrownAction + " vs " + userThrow);
            }

            if (gameOver()) {
                currentState = roundWins > roundLosses ? STATES.WIN : STATES.LOSS;
            } else {
                setTransitionTime(WAIT_FOR_NEW_ROUND_DELAY);
                currentState = STATES.WAIT_FOR_NEW_ROUND;
            }
            lightRingControl.setScore(roundWins, roundLosses);
        } else if (currentState == STATES.WIN) {
            Log.i("RPS_STATE", "STATES.WIN");
            lightRingControl.runSwirl(5, Color.BLUE);
            setTransitionTime(ANIMATION_WAIT_TIME);
            currentState = STATES.WAIT_FOR_NEW_GAME;
        } else if (currentState == STATES.LOSS) {
            Log.i("RPS_STATE", "STATES.LOSS");
            lightRingControl.runSwirl(5, Color.MAGENTA);
            setTransitionTime(ANIMATION_WAIT_TIME);
            currentState = STATES.WAIT_FOR_NEW_GAME;
        } else if (currentState == STATES.WAIT_FOR_NEW_ROUND) {
            Log.i("RPS_STATE", "STATES.WAIT_FOR_NEW_ROUND");
            currentState = nextStateForWaitState(STATES.INITIATE);
        } else if (currentState == STATES.WAIT_FOR_NEW_GAME) {
            Log.i("RPS_STATE", "STATES.WAIT_FOR_NEW_GAME");
            currentState = nextStateForWaitState(STATES.END_GAME);
        } else if (currentState == STATES.END_GAME) {
            Log.i("RPS_STATE", "STATES.END_GAME");
            handController.loose();
            setTransitionTime(ANIMATION_WAIT_TIME);
            currentState = STATES.END_GAME_WAIT;
        } else if (currentState == STATES.END_GAME_WAIT) {
            Log.i("RPS_STATE", "STATES.END_GAME_WAIT");
            currentState = nextStateForWaitState(STATES.GAME_OVER);
        } else if (currentState == STATES.GAME_OVER) {
            lightRingControl.setScore(0, 0);
            if (gameStateListener != null) {
                gameStateListener.gameFinished();
            }
            currentState = STATES.IDLE;
        }
    }

    private boolean gameOver() {
        return roundLosses == 2 || roundWins == 2 ||
                (roundWins + roundLosses) >= 3;
    }

    private GAME_RESULTS getGameResults(String seenAction) {
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
        for (String action : ACTIONS) {
            if (monitoredActions.containsKey(action) && monitoredActions.get(action) >= WIN_SAMPLES_NEEDED) {
                return action;
            }
        }
        return null;
    }

    private void resetGame() {
        roundLosses = 0;
        roundWins = 0;
        lightRingControl.setScore(0, 0);
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
        if (!monitoredActions.containsKey(seenAction)) {
            monitoredActions.put(seenAction, 0);
        }
        Integer oldValue = monitoredActions.get(seenAction);
        monitoredActions.put(seenAction, oldValue + 1);
    }
}
