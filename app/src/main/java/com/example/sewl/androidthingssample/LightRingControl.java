package com.example.sewl.androidthingssample;

import android.graphics.Color;
import android.util.Log;

import com.google.android.things.contrib.driver.apa102.Apa102;

import java.io.IOException;

/**
 * Created by mderrick on 10/16/17.
 */

public class LightRingControl {

    private static final int NUMBER_OF_LEDS         = 12;
    private static final int PULSE_DELAY            = 10;
    private static final int NUMBER_OF_LED_STEPS    = 720;
    public static final String DEFAULT_SPI_BUS      = "SPI3.0";
    private static final float SWIRL_SECONDS        = 1.5f;

    private Thread ledThread;

    private Apa102 mLedstrip;

    private int totalPulsesToRun = 0;

    public LightRingControl() {
    }

    public void init() {
        try {
            mLedstrip = new Apa102(DEFAULT_SPI_BUS, Apa102.Mode.BGR, Apa102.Direction.NORMAL);
            mLedstrip.setBrightness(20);
        } catch (IOException e) { }
    }

    public void runSwirl(int pulsesToRun, final int color, final float totalPulseSeconds) {
        final float[] hsv = new float[3];
        final long totalMillis = Math.round((totalPulseSeconds / (float) (NUMBER_OF_LED_STEPS)) * 1000);
        Log.i("TOTAL", "millis: " + totalMillis);
        Color.RGBToHSV((color >> 16)& 0xFF, (color >> 8) & 0xFF, color & 0xFF, hsv);
        this.totalPulsesToRun = pulsesToRun;
        ledThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int numberOfRuns = 0;
                while(numberOfRuns < totalPulsesToRun) {
                    swirl(hsv[0], totalMillis);
                    numberOfRuns++;
                }
                stopLedThread();
            };
        });
        ledThread.start();
    }

    public void runSwirl(int pulsesToRun, final int color) {
        runSwirl(pulsesToRun, color, SWIRL_SECONDS);
    }

    public void runSwirl(int pulsesToRun) {
        runSwirl(pulsesToRun, Color.BLUE, SWIRL_SECONDS);
    }

    private void swirl(float ledColor, long pulseDelay) {
        int[] colors = new int[NUMBER_OF_LEDS];
        for (int i = 0; i <= NUMBER_OF_LED_STEPS; i+=2) {
            float t = i/(NUMBER_OF_LED_STEPS * 0.5f);
            for (int j = 0; j < NUMBER_OF_LEDS; j++) {
                float offset = (float)j / (float) NUMBER_OF_LEDS;
                // 2*offset for slower ring
                double lightness = 0.3f * Math.sin(t * Math.PI - 2*offset);
                int color = Color.HSVToColor(new float[]{ ledColor, 1.0f, (float) lightness});
                colors[j] = color;
            }

            try {
                mLedstrip.write(colors);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(pulseDelay);
            } catch (InterruptedException e) {
            }
        }
    }

    public void setScore(int me, int them) {
        int[] colors = new int[NUMBER_OF_LEDS];
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
        runPulse(pulses, Color.BLUE);
    }

    public void runPulse(int pulses, int color) {
        stopLedThread();
        totalPulsesToRun = pulses;
        final float[] hsv = new float[3];
        Color.RGBToHSV((color >> 16)& 0xFF, (color >> 8) & 0xFF, color & 0xFF, hsv);
        ledThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int numberOfRuns = 0;
                while(numberOfRuns < totalPulsesToRun) {
                    illuminate(hsv[0]);
                    deluminate(hsv[0]);
                    numberOfRuns++;
                }
                stopLedThread();
            };
        });
        ledThread.start();
    }

    private void illuminate(float ledColor) {
        int[] colors = new int[NUMBER_OF_LEDS];
        for (int i = 0; i < 170; i+=2) {
            float t = i/360.0f;
            for (int j = 0; j < NUMBER_OF_LEDS; j++) {
                int color = Color.HSVToColor(new float[]{ ledColor, 1.0f, t});
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

    private void deluminate(float ledColor) {
        int[] colors = new int[NUMBER_OF_LEDS];
        for (int i = 170; i >= 0; i-=2) {
            float t = i/360.0f;
            for (int j = 0; j < NUMBER_OF_LEDS; j++) {
                int color = Color.HSVToColor(new float[]{ ledColor, 1.0f, t});
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
            try {
                mLedstrip.write(new int[NUMBER_OF_LEDS]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
