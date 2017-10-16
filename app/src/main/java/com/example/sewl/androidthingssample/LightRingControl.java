package com.example.sewl.androidthingssample;

import android.graphics.Color;
import android.os.Handler;
import android.os.Message;

import com.google.android.things.contrib.driver.apa102.Apa102;

import java.io.IOException;

/**
 * Created by mderrick on 10/16/17.
 */

public class LightRingControl {

    private static final int NUMBER_OF_LEDS = 4;
    private static final int PULSE_DELAY    = 8;

    private final Handler localHandler;

    private Thread ledThread;

    private Apa102 mLedstrip;

    private int totalPulsesToRun = 0;

    public LightRingControl(Handler localHandler) {
        this.localHandler = localHandler;
    }

    public void init() {
        try {
            mLedstrip = new Apa102("SPI3.0", Apa102.Mode.BGR, Apa102.Direction.NORMAL);
            mLedstrip.setBrightness(1);
        } catch (IOException e) { }
    }

    public void setScore(int me, int them) {
        int[] colors = new int[] {0, 0, 0, 0};
        for (int i = 0; i < me; i++) {
            colors[i] = Color.BLUE;
        }
        for (int i = me; i < me + them; i++) {
            colors[i] = Color.MAGENTA;
        }
        try {
            mLedstrip.write(colors);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void runPulse(int pulses) {
        totalPulsesToRun = pulses;
        ledThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int numberOfRuns = 0;
                while(numberOfRuns < totalPulsesToRun) {
                    illuminate();
                    deluminate();
                    numberOfRuns++;
                    Message msg = new Message();
                    msg.obj = "RUNS: " + numberOfRuns;
                    localHandler.sendMessage(msg);
                }
                stopLedThread();
            };
        });
        ledThread.start();
    }

    private void illuminate() {
        int[] colors = new int[] {0, 0, 0, 0};
        for (int i = 0; i < 150; i++) {
            float t = i/360.0f;
            for (int j = 0; j < NUMBER_OF_LEDS; j++) {
                int color = Color.HSVToColor(new float[]{ 200.0f, 1.0f, t});
                colors[j] = color;
            }
            try {
                mLedstrip.write(colors);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(PULSE_DELAY);
            } catch (InterruptedException e) {
            }
        }
    }

    private void deluminate() {
        int[] colors = new int[] {0, 0, 0, 0};
        for (int i = 150; i >= 0; i--) {
            float t = i/360.0f;
            for (int j = 0; j < NUMBER_OF_LEDS; j++) {
                int color = Color.HSVToColor(new float[]{ 200.0f, 1.0f, t});
                colors[j] = color;
            }
            try {
                mLedstrip.write(colors);
            } catch (IOException e) {
            }

            try {
                Thread.sleep(PULSE_DELAY);
            } catch (InterruptedException e) {
            }
        }
    }

    private void stopLedThread() {
        if (ledThread != null) {
            ledThread.interrupt();
            ledThread = null;
        }
    }
}
