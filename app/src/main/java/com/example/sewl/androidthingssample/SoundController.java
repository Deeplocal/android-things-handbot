package com.example.sewl.androidthingssample;

import android.content.Context;
import android.media.MediaPlayer;

/**
 * Created by mderrick on 10/16/17.
 */

public class SoundController {

    public enum SOUNDS {
        WIN,
        LOSS,
        CORRECT,
        INCORRECT
    }

    private Context context;

    public SoundController(Context context) {
        this.context = context;
    }

    public void playSound(SOUNDS sound) {
        if (sound == SOUNDS.CORRECT) {
            play(R.raw.success_tone);
        } else if (sound == SOUNDS.INCORRECT) {
            play(R.raw.error_tone);
        }
    }

    public void playSignSound(String action) {
        if ("rock".equals(action)) {
            play(R.raw.c_l_tone);
        } else if ("scissors".equals(action)) {
            play(R.raw.c_h_tone);
        } else if ("paper".equals(action)) {
            play(R.raw.a_tone);
        } else if ("spiderman".equals(action)) {
            play(R.raw.g_tone);
        } else {
            play(R.raw.b_tone);
        }
    }

    private void play(int sound) {
        MediaPlayer mediaPlayer = MediaPlayer.create(context, sound);
        mediaPlayer.setVolume(0, 0.9f);
        mediaPlayer.setLooping(false);
        mediaPlayer.start();
    }
}
