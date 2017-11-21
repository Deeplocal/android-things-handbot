package com.example.sewl.androidthingssample;

import android.content.Context;
import android.media.MediaPlayer;

/**
 * Created by mderrick on 10/16/17.
 */

public class SoundController {

    public enum SOUNDS {
        START_GAME,
        TIE,
        WIN,
        LOSS,
        CORRECT,
        INCORRECT,
        RPS_BING,
        RPS_BONG,
        ROUND_WIN,
        ROUND_LOSS,
        MIRROR
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
        } else if (sound == SOUNDS.START_GAME) {
            play(R.raw.start_game);
        } else if (sound == SOUNDS.RPS_BING) {
            play(R.raw.c_l_tone);
        } else if (sound == SOUNDS.RPS_BONG) {
            play(R.raw.c_h_tone);
        } else if (sound == SOUNDS.TIE) {
            play(R.raw.error_tone);
        } else if (sound == SOUNDS.WIN) {
            play(R.raw.game_win);
        } else if (sound == SOUNDS.LOSS) {
            play(R.raw.game_loss);
        } else if (sound == SOUNDS.ROUND_WIN) {
            play(R.raw.success_tone);
        } else if (sound == SOUNDS.MIRROR) {
            play(R.raw.mirror_simon_says);
        } else if (sound == SOUNDS.ROUND_LOSS) {
            play(R.raw.round_loss);
        }
    }

    public void playSignSound(String action) {
        if (Signs.ROCK.equals(action)) {
            play(R.raw.c_l_tone);
        } else if (Signs.SCISSORS.equals(action)) {
            play(R.raw.c_h_tone);
        } else if (Signs.PAPER.equals(action)) {
            play(R.raw.a_tone);
        } else if (Signs.SPIDERMAN.equals(action)) {
            play(R.raw.g_tone);
        } else {
            play(R.raw.b_tone);
        }
    }

    private void play(int sound) {
        MediaPlayer mediaPlayer = MediaPlayer.create(context, sound);
        mediaPlayer.setVolume(BoardDefaults.DEFAULT_VOLUME, BoardDefaults.DEFAULT_VOLUME);
        mediaPlayer.setLooping(false);
        mediaPlayer.start();
    }
}
