package com.example.sewl.androidthingssample;

import android.graphics.Color;
import android.util.Log;

import com.google.android.things.contrib.driver.apa102.*;

import java.io.IOException;

/**
 * Created by mderrick on 10/16/17.
 */

public class LightRingControl {

    private static final String TAG = LightRingControl.class.getSimpleName();

    public static final long FAST_PULSE_ANIMATION_STEP_MILLIS = 4;
    private static final int NUMBER_OF_LEDS                     = 5;
    public static final int LEDS_PER_RPS_SCORE_MARK             = NUMBER_OF_LEDS / 3;
    public static final int LEDS_PER_MATCHING_SCORE_MARK        = NUMBER_OF_LEDS / 8;
    private static final int PULSE_DELAY                        = 10;
    private static final int NUMBER_OF_LED_STEPS                = 720;
    private static final float SWIRL_SECONDS                    = 0.5f;

    private static final long FLASH_DELAY                       = 1;
    public static final int MILLIS_PER_SECOND                   = 1000;
    public static final float SWIRL_BRIGHTNESS_AMPLITUDE_MAX    = 0.3f;
    public static final float MAX_COLOR_ALPHA                   = 1.0f;
    public static final int PLAYER_SCORE_LEDS_PER_SECTION       = 8;
    public static final float PLAYER_SCORE_HSV_VALUE            = 0.6f;
    public static final float LED_RING_START_OFFSET_PERCENTAGE  = 0.70f;
    public static final int MAX_SCORE_DEGREES_OF_HSV_VALUE = 170;
    public static final float DEGREES_IN_A_CIRCLE               = 360.0f;
    public static final int LED_RING_START_OFFSET_INDEX         = Math.round(NUMBER_OF_LEDS * LED_RING_START_OFFSET_PERCENTAGE);
    private float[] GREEN_HSV                                   = new float[3];
    private float[] RED_HSV                                     = new float[3];

    private boolean flag_busy = false;

    private Thread ledThread;

    private Apa102 ledstrip;

    private int totalPulsesToRun = 0;

    public LightRingControl() {
    }

    public void init() {
        try {
            ledstrip = new Apa102(BoardDefaults.DEFAULT_SPI_BUS, Apa102.Mode.RBG, Apa102.Direction.NORMAL);
            ledstrip.setBrightness(BoardDefaults.LED_BRIGHTNESS);

            // Calculate HSV once for colors
            Color.RGBToHSV(Color.red(Color.RED), Color.green(Color.RED), Color.blue(Color.RED), RED_HSV);
            Color.RGBToHSV(Color.red(Color.GREEN), Color.green(Color.GREEN), Color.blue(Color.GREEN), GREEN_HSV);
        } catch (IOException e) { }
    }

    public boolean isBusy() {
        return flag_busy;
    }

    public void runSwirl(int pulsesToRun, final int color, final float totalPulseSeconds) {
        final float[] hsv = new float[3];
        final long totalMillis = Math.round((totalPulseSeconds / (float) (NUMBER_OF_LED_STEPS)) * MILLIS_PER_SECOND);
        Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), hsv);
        this.totalPulsesToRun = pulsesToRun;
        ledThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int numberOfRuns = 0;
                flag_busy=true;
                while(numberOfRuns < totalPulsesToRun) {
                    swirl(hsv[0], totalMillis);
                    numberOfRuns++;
                }
                flag_busy=false;
                stopLedThread();
            };
        });
        ledThread.start();
    }

    public void runSwirl(int pulsesToRun, final int color) {
        runSwirl(pulsesToRun, color, SWIRL_SECONDS);
    }

    private void swirl(float ledColor, long pulseDelay) {
        int[] colors = new int[NUMBER_OF_LEDS];
        for (int i = 0; i <= NUMBER_OF_LED_STEPS; i+=2) {
            float t = i/(NUMBER_OF_LED_STEPS * 0.5f);
            for (int j = 0; j < NUMBER_OF_LEDS; j++) {
                float offset = (float)j / (float) NUMBER_OF_LEDS;
                double lightness = SWIRL_BRIGHTNESS_AMPLITUDE_MAX * Math.sin(t * Math.PI - 2*offset);
                int color = Color.HSVToColor(new float[]{ ledColor, MAX_COLOR_ALPHA, (float) lightness});
                colors[j] = color;
            }
            writeToLEDStrip(colors);
            sleep(pulseDelay);
        }
    }

    public void setRPSScore(int me, int them) {
        int ledsPerMark = LEDS_PER_RPS_SCORE_MARK;
        int[] colors = new int[NUMBER_OF_LEDS];

        for (int i = 0; i < me*ledsPerMark; i++) {
            colors[i] = Color.GREEN;
        }
        for (int i = NUMBER_OF_LEDS - 1; i > (NUMBER_OF_LEDS - 1 - them*ledsPerMark); i--) {
            colors[i] = Color.RED;
        }
        writeToLEDStrip(colors);
    }

    public void showMatchingLights(int number, int wrong) {
        int[] colors = new int[NUMBER_OF_LEDS];
        setLEDsForCorrectAndIncorrect(colors, number, wrong, 1.0f);
    }

    private void writeToLEDStrip(int[] colors) {
        try {
            ledstrip.write(colors);
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to LED strip", e);
        }
    }

    private int shiftLedIndex(int index) {
        return (index + LED_RING_START_OFFSET_INDEX) % NUMBER_OF_LEDS;
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
                flag_busy=true;
                while(numberOfRuns < totalPulsesToRun) {
                    illuminate(hsv[0], PULSE_DELAY);
                    deluminate(hsv[0], PULSE_DELAY);
                    numberOfRuns++;
                }
                flag_busy=false;
                stopLedThread();
            };
        });
        ledThread.start();
    }

    public void flash(int pulses, int color) {
        stopLedThread();
        totalPulsesToRun = pulses;
        final float[] hsv = new float[3];
        Color.RGBToHSV((color >> 16)& 0xFF, (color >> 8) & 0xFF, color & 0xFF, hsv);
        ledThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int numberOfRuns = 0;
                flag_busy=true;
                while(numberOfRuns < totalPulsesToRun) {
                    illuminate(hsv[0], FLASH_DELAY);
                    deluminate(hsv[0], FLASH_DELAY);
                    numberOfRuns++;
                }
                flag_busy=false;
                stopLedThread();
            };
        });
        ledThread.start();
    }

    public void runScorePulse(int pulses, final int correct, final int incorrect) {
        stopLedThread();
        totalPulsesToRun = pulses;

        ledThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int numberOfRuns = 0;
                flag_busy=true;
                while(numberOfRuns < totalPulsesToRun) {
                    int[] colors = new int[NUMBER_OF_LEDS];
                    for (int i = 0; i < MAX_SCORE_DEGREES_OF_HSV_VALUE; i+=2) {
                        animateLEDsForCorrectAndIncorrect(colors, i, correct, incorrect);
                    }

                    for (int i = MAX_SCORE_DEGREES_OF_HSV_VALUE; i >= 0; i-=2) {
                        animateLEDsForCorrectAndIncorrect(colors, i, correct, incorrect);
                    }
                    numberOfRuns++;
                }
                flag_busy=false;
                stopLedThread();
            }
        });
        ledThread.start();
    }

    private void animateLEDsForCorrectAndIncorrect(int[] colors, int HSVValueDegree, int correctLEDs, int incorrectLEDs) {
        float t = HSVValueDegree/ DEGREES_IN_A_CIRCLE;
        setLEDsForCorrectAndIncorrect(colors, correctLEDs, incorrectLEDs, t);
        sleep(FAST_PULSE_ANIMATION_STEP_MILLIS);
    }

    private void setLEDsForCorrectAndIncorrect(int[] colors, int correctLEDs, int incorrectLEDs, float t) {
        int pulseGreen = Color.HSVToColor(new float[]{ GREEN_HSV[0], 1.0f, t*PLAYER_SCORE_HSV_VALUE});
        for (int j = 0; j < correctLEDs; j++) {
            colors[shiftLedIndex(j * LEDS_PER_MATCHING_SCORE_MARK)] = Color.BLACK;
            colors[shiftLedIndex(j * LEDS_PER_MATCHING_SCORE_MARK + 1)] = pulseGreen;
            colors[shiftLedIndex(j * LEDS_PER_MATCHING_SCORE_MARK + 2)] = Color.BLACK;
        }
        int pulseRed = Color.HSVToColor(new float[] { RED_HSV[0], 1.0f, t*PLAYER_SCORE_HSV_VALUE});
        for (int j = correctLEDs; j < correctLEDs + incorrectLEDs; j++) {
            colors[shiftLedIndex(j * LEDS_PER_MATCHING_SCORE_MARK)] = Color.BLACK;
            colors[shiftLedIndex(j * LEDS_PER_MATCHING_SCORE_MARK + 1)] = pulseRed;
            colors[shiftLedIndex(j * LEDS_PER_MATCHING_SCORE_MARK + 2)] = Color.BLACK;
        }
        writeToLEDStrip(colors);
    }

    private void illuminate(float ledColor, long delay) {
        int[] colors = new int[NUMBER_OF_LEDS];
        for (int i = 0; i < 170; i+=2) {
            float t = i/DEGREES_IN_A_CIRCLE;
            for (int j = 0; j < NUMBER_OF_LEDS; j++) {
                int color = Color.HSVToColor(new float[]{ ledColor, 1.0f, t});
                colors[j] = color;
            }

            writeToLEDStrip(colors);
            sleep(delay);
        }
    }

    private void deluminate(float ledColor, long delay) {
        int[] colors = new int[NUMBER_OF_LEDS];
        for (int i = 170; i >= 0; i-=2) {
            float t = i/ DEGREES_IN_A_CIRCLE;
            for (int j = 0; j < NUMBER_OF_LEDS; j++) {
                int color = Color.HSVToColor(new float[]{ ledColor, 1.0f, t});
                colors[j] = color;
            }
            writeToLEDStrip(colors);

            sleep(delay);
        }
    }

    private void sleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Log.e(TAG, "Failed to sleep", e);
        }
    }

    private void stopLedThread() {
        if (ledThread != null) {
            ledThread.interrupt();
            ledThread = null;
            writeToLEDStrip(new int[NUMBER_OF_LEDS]);
        }
    }

    public void setColor(int color) {
        int[] colors = new int[NUMBER_OF_LEDS];

        for (int i = 0; i < NUMBER_OF_LEDS; i++) {
            colors[i] = color;
        }
        while(flag_busy){};
        sleep(100);
        writeToLEDStrip(colors);
    }
}
