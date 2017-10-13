package com.example.sewl.androidthingssample;

/**
 * Created by mderrick on 10/12/17.
 */

public interface Game {

    void run(String action);
    void start();
    void stop();
    void shutdown();
}
