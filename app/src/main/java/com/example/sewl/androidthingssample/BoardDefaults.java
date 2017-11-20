package com.example.sewl.androidthingssample;

/**
 * Created by mderrick on 11/20/17.
 */

public interface BoardDefaults {

    interface HandPinout {
        int THUMB           = 8;
        int RING            = 0;
        int MIDDLE          = 2;
        int INDEX           = 4;
        int PINKY           = 5;
        int WRIST           = 9;

        // If facing the hand to play RPS
        int FOREARM_ON_USER_RIGHT = 12;
        int FOREARM_ON_USER_LEFT    = 13;
    }
}
