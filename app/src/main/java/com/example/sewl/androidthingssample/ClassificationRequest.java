package com.example.sewl.androidthingssample;

import android.graphics.Bitmap;

/**
 * Created by mderrick on 10/31/17.
 */

public class ClassificationRequest {

    private Bitmap bitmap;

    private String action;

    public ClassificationRequest(Bitmap bitmap, String action) {
        this.bitmap = bitmap;
        this.action = action;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
