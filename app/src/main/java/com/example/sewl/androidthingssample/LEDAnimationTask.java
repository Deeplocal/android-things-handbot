package com.example.sewl.androidthingssample;

import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.things.contrib.driver.apa102.Apa102;

import java.io.IOException;

/**
 * Created by mderrick on 10/16/17.
 */

public class LEDAnimationTask extends AsyncTask<Apa102, Void, Void>  {

    private static int NUMBER_OF_LEDS = 4;

    @Override
    protected Void doInBackground(Apa102... apa102s) {
        boolean run = true;
        long lastMillis = 0;

        while (run) {
            long currentMillis = System.currentTimeMillis();
            Apa102 ledStrip = apa102s[0];
            int[] colors = new int[] {0, 0, 0, 0};
            int i = 0;
            if (currentMillis - lastMillis > 10 ) {
                lastMillis = currentMillis;
                float t = i/360.0f;
                Log.i("T", "t: " + t);
                for (int j = 0; j < NUMBER_OF_LEDS; j++) {
                    int color = Color.HSVToColor(new float[]{ 360.0f*t, 1.0f, 1.0f});
                    colors[j] = color;
                }
                try {
                    ledStrip.write(colors);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                i++;
                if (i >= 359) {
                    run = false;
                }
            }
        }
        return null;
    }
}
