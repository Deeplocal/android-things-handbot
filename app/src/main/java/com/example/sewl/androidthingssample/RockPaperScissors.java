package com.example.sewl.androidthingssample;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mderrick on 10/11/17.
 */

public class RockPaperScissors {

    public static final int WIN_SAMPLES_NEEDED = 3;
    private HandController handController;

    private Map<String, Integer> monitoredActions = new HashMap<>();

    private int actionsOfInterest;

    private int totalRecordedActions;

    private int roundLosses;

    private int roundWins;

    private String[] ACTIONS = new String[] { "rock", "paper", "scissors" };

    private String thrownAction;

    public RockPaperScissors(HandController handController) {
        this.handController = handController;
    }

    private enum STATES {
        IDLE,
        INITIATE,
        COUNTDOWN,
        THROW,
        MONITOR,
        DETERMINE_ROUND_WINNER,
        WIN,
        LOSS,
    }

    private enum GAME_RESULTS {
        WIN,
        LOSS,
        TIE
    }

    private STATES currentState;

    public void init() {
        this.currentState = STATES.IDLE;
    }

    public void start() {
        currentState = STATES.INITIATE;
    }

    public void stop() {
        currentState = STATES.IDLE;
    }

    public void run(String seenAction) {
        if (currentState == STATES.IDLE) {
            roundLosses = 0;
            roundWins = 0;
        } else if (currentState == STATES.INITIATE) {
            handController.moveToRPSReady();
            currentState = STATES.COUNTDOWN;
        } else if (currentState == STATES.COUNTDOWN) {
            handController.countDown();
            currentState = STATES.THROW;
        } else if (currentState == STATES.THROW) {
            handController.throwRPSAction(seenAction);
            actionsOfInterest = 0;
            totalRecordedActions = 0;
            thrownAction = ACTIONS[(int)(Math.random() * ACTIONS.length)];
            currentState = STATES.MONITOR;
        } else if (currentState == STATES.MONITOR) {
            logAction(seenAction);
            if (getUserThrow() != null) {
                currentState = STATES.DETERMINE_ROUND_WINNER;
            } else if (totalRecordedActions >= 30) {
                currentState = STATES.LOSS;
            }
            totalRecordedActions++;
        } else if (currentState == STATES.DETERMINE_ROUND_WINNER) {
            String userThrow = getUserThrow();
            GAME_RESULTS gameResults = getGameResults(userThrow);
            if (gameResults == GAME_RESULTS.WIN) {
                roundWins++;
            } else if (gameResults == GAME_RESULTS.LOSS) {
                roundLosses++;
            }

            if (roundWins + roundLosses >= 3) {
                currentState = roundWins > roundLosses ? STATES.WIN : STATES.LOSS;
            } else {
                currentState = STATES.INITIATE;
            }
        } else if (currentState == STATES.WIN) {
            handController.thumbsUp();
            currentState = STATES.IDLE;
        } else if (currentState == STATES.LOSS) {
            handController.thumbsDown();
            currentState = STATES.IDLE;
        }
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

    private void logAction(String seenAction) {
        if (!monitoredActions.containsKey(seenAction)) {
            monitoredActions.put(seenAction, 0);
        }
        Integer oldValue = monitoredActions.get(seenAction);
        monitoredActions.put(seenAction, oldValue + 1);
    }
}
