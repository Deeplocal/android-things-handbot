package com.example.sewl.androidthingssample;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by mderrick on 11/7/17.
 */

public class SettingsRepository {

    private static final String FOREARM_SERVO_OFFSET_KEY     = "FOREARM_SERVO_OFFSET_KEY";
    private static final String SERVO_OFFSETS_REPOSITORY_KEY = "SERVO_OFFSETS_REPOSITORY_KEY";

    private Context context;

    public SettingsRepository(Context context) {
        this.context = context;
    }

    public void saveForearmServoOffset(int offset) {
        getSharedPrefs()
                .edit()
                .putInt(FOREARM_SERVO_OFFSET_KEY, offset)
                .commit();
    }

    public int getForearmServoOffset() {
        return getSharedPrefs()
                .getInt(FOREARM_SERVO_OFFSET_KEY, ForearmController.DEFAULT_MOTOR_SYNC_OFFSET);
    }

    private SharedPreferences getSharedPrefs() {
        return context.getSharedPreferences(SERVO_OFFSETS_REPOSITORY_KEY, Context.MODE_PRIVATE);
    }

    public void incrementForearmOffset() {
        int offset = getForearmServoOffset();
        offset += 5;
        if (offset >= 100) {
            offset = ForearmController.DEFAULT_MOTOR_SYNC_OFFSET;
        }
        saveForearmServoOffset(offset);
    }
}
