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
            play(R.raw.success);
        } else if (sound == SOUNDS.INCORRECT) {
            play(R.raw.error);
        }
    }

    private void play(int sound) {
        MediaPlayer mediaPlayer = MediaPlayer.create(context, sound);
        mediaPlayer.setVolume(0, 0.9f);
        mediaPlayer.setLooping(false);
        mediaPlayer.start();
    }
}
