package com.example.sewl.androidthingssample;

import java.util.List;

/**
 * Created by mderrick on 10/12/17.
 */

public interface Game {
    void run(String action, List<Classifier.Recognition> results);
    void start();
    void stop();
    void shutdown();
    String getClassifierKey();
}
