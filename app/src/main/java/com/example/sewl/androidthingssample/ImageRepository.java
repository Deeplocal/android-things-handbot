package com.example.sewl.androidthingssample;

import android.graphics.Bitmap;
import android.media.Image;

import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Created by mderrick on 10/4/17.
 */

public interface ImageRepository {

    @POST("/api/images")
    Call<ResponseBody> saveImage(@Body ImagePost image);
}
