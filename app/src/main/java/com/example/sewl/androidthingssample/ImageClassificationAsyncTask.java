package com.example.sewl.androidthingssample;

import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;

import java.util.List;

/**
 * Created by mderrick on 10/12/17.
 */

public class ImageClassificationAsyncTask extends AsyncTask<ImageReader, Void, String> {

    private ImagePreprocessor mImagePreprocessor;

    private TensorFlowImageClassifier mTensorFlowClassifier;

    private ClassificationAvailableListener listener;

    public ImageClassificationAsyncTask(ImagePreprocessor mImagePreprocessor,
                                        TensorFlowImageClassifier mTensorFlowClassifier,
                                        ClassificationAvailableListener listener) {
        this.mImagePreprocessor = mImagePreprocessor;
        this.mTensorFlowClassifier = mTensorFlowClassifier;
        this.listener = listener;
    }

    @Override
    protected String doInBackground(ImageReader... imageReaders) {
        final Bitmap bitmap;
        try (Image image = imageReaders[0].acquireNextImage()) {
            bitmap = mImagePreprocessor.preprocessImage(image);
        }
        final List<Classifier.Recognition> results = mTensorFlowClassifier.doRecognize(bitmap);
        if (listener != null) {
            listener.onImageClassificationAvailable(results);
        }
        return null;
    }

    public interface ClassificationAvailableListener {
        void onImageClassificationAvailable(List<Classifier.Recognition> classifications);
    }
}
