/*
 * Copyright 2020 The TensorFlow Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.superresolution;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.WorkerThread;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import com.google.android.gms.common.Feature;
import com.google.firebase.database.annotations.Nullable;

/** A super resolution class to generate super resolution images from low resolution images * */
public class MainActivity extends AppCompatActivity {
  static {
    System.loadLibrary("SuperResolution");
  }

  private static final String TAG = "SuperResolution";
  private static final String MODEL_NAME = "ESRGAN.tflite";
  private static final int LR_IMAGE_HEIGHT = 50;
  private static final int LR_IMAGE_WIDTH = 50;
  private static final int UPSCALE_FACTOR = 4;
  private static final int SR_IMAGE_HEIGHT = LR_IMAGE_HEIGHT * UPSCALE_FACTOR;
  private static final int SR_IMAGE_WIDTH = LR_IMAGE_WIDTH * UPSCALE_FACTOR;
  private static final String LR_IMG_1 = "lr-1.jpg";
  private static final String LR_IMG_2 = "lr-2.jpg";
  private static final String LR_IMG_3 = "lr-3.jpg";
  private static final String LR_IMG_4 = "lr-4.jpg";
  private static final String LR_IMG_5 = "lr-5.jpg";
  private static final String LR_IMG_6 = "lr-6.jpg";
  private static final String LR_IMG_7 = "lr-7.jpg";
  private static final String LR_IMG_8 = "lr-8.jpg";
  private static final String LR_IMG_9 = "lr-9.jpg";

  private MappedByteBuffer model;
  private long superResolutionNativeHandle = 0;
  private Bitmap selectedLRBitmap = null;
  private boolean useGPU = false;

  private ImageView lowResImageView1;
  private ImageView lowResImageView2;
  private ImageView lowResImageView3;
  private ImageView lowResImageView4;
  private ImageView lowResImageView5;
  private ImageView lowResImageView6;
  private ImageView lowResImageView7;
  private ImageView lowResImageView8;
  private ImageView lowResImageView9;
  private TextView selectedImageTextView;
  private Switch gpuSwitch;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    final Button superResolutionButton = findViewById(R.id.upsample_button);
    lowResImageView1 = findViewById(R.id.low_resolution_image_1);
    lowResImageView2 = findViewById(R.id.low_resolution_image_2);
    lowResImageView3 = findViewById(R.id.low_resolution_image_3);
    lowResImageView4 = findViewById(R.id.low_resolution_image_4);
    lowResImageView5 = findViewById(R.id.low_resolution_image_5);
    lowResImageView6 = findViewById(R.id.low_resolution_image_6);
    lowResImageView7 = findViewById(R.id.low_resolution_image_7);
    lowResImageView8 = findViewById(R.id.low_resolution_image_8);
    lowResImageView9 = findViewById(R.id.low_resolution_image_9);
    selectedImageTextView = findViewById(R.id.chosen_image_tv);
    gpuSwitch = findViewById(R.id.switch_use_gpu);

    ImageView[] lowResImageViews = {lowResImageView1, lowResImageView2, lowResImageView3,
                                    lowResImageView4, lowResImageView5, lowResImageView6,
                                    lowResImageView7, lowResImageView8, lowResImageView9};

    AssetManager assetManager = getAssets();
    try {
      InputStream inputStream1 = assetManager.open(LR_IMG_1);
      Bitmap bitmap1 = BitmapFactory.decodeStream(inputStream1);
      lowResImageView1.setImageBitmap(bitmap1);

      InputStream inputStream2 = assetManager.open(LR_IMG_2);
      Bitmap bitmap2 = BitmapFactory.decodeStream(inputStream2);
      lowResImageView2.setImageBitmap(bitmap2);

      InputStream inputStream3 = assetManager.open(LR_IMG_3);
      Bitmap bitmap3 = BitmapFactory.decodeStream(inputStream3);
      lowResImageView3.setImageBitmap(bitmap3);

      InputStream inputStream4 = assetManager.open(LR_IMG_4);
      Bitmap bitmap4 = BitmapFactory.decodeStream(inputStream4);
      lowResImageView4.setImageBitmap(bitmap4);

      InputStream inputStream5 = assetManager.open(LR_IMG_5);
      Bitmap bitmap5 = BitmapFactory.decodeStream(inputStream5);
      lowResImageView5.setImageBitmap(bitmap5);

      InputStream inputStream6 = assetManager.open(LR_IMG_6);
      Bitmap bitmap6 = BitmapFactory.decodeStream(inputStream6);
      lowResImageView6.setImageBitmap(bitmap6);

      InputStream inputStream7 = assetManager.open(LR_IMG_7);
      Bitmap bitmap7 = BitmapFactory.decodeStream(inputStream7);
      lowResImageView7.setImageBitmap(bitmap7);

      InputStream inputStream8 = assetManager.open(LR_IMG_8);
      Bitmap bitmap8 = BitmapFactory.decodeStream(inputStream8);
      lowResImageView8.setImageBitmap(bitmap8);

      InputStream inputStream9 = assetManager.open(LR_IMG_9);
      Bitmap bitmap9 = BitmapFactory.decodeStream(inputStream9);
      lowResImageView9.setImageBitmap(bitmap9);
    } catch (IOException e) {
      Log.e(TAG, "Failed to open an low resolution image");
    }

    for (ImageView iv : lowResImageViews) {
      setLRImageViewListener(iv);
    }

    superResolutionButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            if (selectedLRBitmap == null) {
              Toast.makeText(
                      getApplicationContext(),
                      "Please choose one low resolution image",
                      Toast.LENGTH_LONG)
                  .show();
              return;
            }

            if (superResolutionNativeHandle == 0) {
                superResolutionNativeHandle = initTFLiteInterpreter(gpuSwitch.isChecked());
            } else if (useGPU != gpuSwitch.isChecked()) {
              // We need to reinitialize interpreter when execution hardware is changed
              deinit();
              superResolutionNativeHandle = initTFLiteInterpreter(gpuSwitch.isChecked());
            }
            useGPU = gpuSwitch.isChecked();
            if (superResolutionNativeHandle == 0) {
              showToast("TFLite interpreter failed to create!");
              return;
            }

            int[] lowResRGB = new int[LR_IMAGE_HEIGHT * LR_IMAGE_WIDTH];
            selectedLRBitmap.getPixels(
                lowResRGB, 0, LR_IMAGE_WIDTH, 0, 0, LR_IMAGE_WIDTH, LR_IMAGE_HEIGHT);

            final long startTime = SystemClock.uptimeMillis();
            int[] superResRGB = doSuperResolution(lowResRGB);
            final long processingTimeMs = SystemClock.uptimeMillis() - startTime;
            if (superResRGB == null) {
              showToast("Super resolution failed!");
              return;
            }

            final LinearLayout resultLayout = findViewById(R.id.result_layout);
            final ImageView superResolutionImageView = findViewById(R.id.super_resolution_image);
            final ImageView nativelyScaledImageView = findViewById(R.id.natively_scaled_image);
            final TextView superResolutionTextView = findViewById(R.id.super_resolution_tv);
            final TextView nativelyScaledImageTextView =
                findViewById(R.id.natively_scaled_image_tv);
            final TextView logTextView = findViewById(R.id.log_view);

            // Force refreshing the ImageView
            superResolutionImageView.setImageDrawable(null);
            Bitmap srImgBitmap =
                Bitmap.createBitmap(
                    superResRGB, SR_IMAGE_WIDTH, SR_IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
            superResolutionImageView.setImageBitmap(srImgBitmap);
            nativelyScaledImageView.setImageBitmap(selectedLRBitmap);
            resultLayout.setVisibility(View.VISIBLE);
            logTextView.setText("Inference time: " + processingTimeMs + "ms");
          }
        });
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    deinit();
  }

  private void setLRImageViewListener(ImageView iv) {
    iv.setOnTouchListener(
        new View.OnTouchListener() {
          @Override
          public boolean onTouch(View v, MotionEvent event) {
            if (v.equals(lowResImageView1)) {
              selectedLRBitmap = ((BitmapDrawable) lowResImageView1.getDrawable()).getBitmap();
              selectedImageTextView.setText(
                  "You are using low resolution image: 1 ("
                      + getResources().getString(R.string.low_resolution_1)
                      + ")");
            } else if (v.equals(lowResImageView2)) {
              selectedLRBitmap = ((BitmapDrawable) lowResImageView2.getDrawable()).getBitmap();
              selectedImageTextView.setText(
                  "You are using low resolution image: 2 ("
                      + getResources().getString(R.string.low_resolution_2)
                      + ")");
            } else if (v.equals(lowResImageView3)) {
              selectedLRBitmap = ((BitmapDrawable) lowResImageView3.getDrawable()).getBitmap();
              selectedImageTextView.setText(
                  "You are using low resolution image: 3 ("
                      + getResources().getString(R.string.low_resolution_3)
                      + ")");
            } else if (v.equals(lowResImageView4)) {
              selectedLRBitmap = ((BitmapDrawable) lowResImageView4.getDrawable()).getBitmap();
              selectedImageTextView.setText(
                      "You are using low resolution image: 4 ("
                              + getResources().getString(R.string.low_resolution_4)
                              + ")");
            } else if (v.equals(lowResImageView5)) {
              selectedLRBitmap = ((BitmapDrawable) lowResImageView5.getDrawable()).getBitmap();
              selectedImageTextView.setText(
                      "You are using low resolution image: 5 ("
                              + getResources().getString(R.string.low_resolution_5)
                              + ")");
            } else if (v.equals(lowResImageView6)) {
              selectedLRBitmap = ((BitmapDrawable) lowResImageView6.getDrawable()).getBitmap();
              selectedImageTextView.setText(
                      "You are using low resolution image: 6 ("
                              + getResources().getString(R.string.low_resolution_6)
                              + ")");
            } else if (v.equals(lowResImageView7)) {
              selectedLRBitmap = ((BitmapDrawable) lowResImageView7.getDrawable()).getBitmap();
              selectedImageTextView.setText(
                      "You are using low resolution image: 7 ("
                              + getResources().getString(R.string.low_resolution_7)
                              + ")");
            } else if (v.equals(lowResImageView8)) {
              selectedLRBitmap = ((BitmapDrawable) lowResImageView8.getDrawable()).getBitmap();
              selectedImageTextView.setText(
                      "You are using low resolution image: 8 ("
                              + getResources().getString(R.string.low_resolution_8)
                              + ")");
            } else if (v.equals(lowResImageView9)) {
              selectedLRBitmap = ((BitmapDrawable) lowResImageView9.getDrawable()).getBitmap();
              selectedImageTextView.setText(
                      "You are using low resolution image: 9 ("
                              + getResources().getString(R.string.low_resolution_9)
                              + ")");
            }
            return false;
          }
        });
  }

  @WorkerThread
  public synchronized int[] doSuperResolution(int[] lowResRGB) {
    return superResolutionFromJNI(superResolutionNativeHandle, lowResRGB);
  }

  private MappedByteBuffer loadModelFile() throws IOException {
    try (AssetFileDescriptor fileDescriptor =
            AssetsUtil.getAssetFileDescriptorOrCached(getApplicationContext(), MODEL_NAME);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {
      FileChannel fileChannel = inputStream.getChannel();
      long startOffset = fileDescriptor.getStartOffset();
      long declaredLength = fileDescriptor.getDeclaredLength();
      return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
  }

  private void showToast(String str) {
    Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
  }

  private long initTFLiteInterpreter(boolean useGPU) {
    try {
      model = loadModelFile();
    } catch (IOException e) {
      Log.e(TAG, "Fail to load model", e);
    }
    return initWithByteBufferFromJNI(model, useGPU);
  }

  private void deinit() {
    deinitFromJNI(superResolutionNativeHandle);
  }

  private native int[] superResolutionFromJNI(long superResolutionNativeHandle, int[] lowResRGB);

  private native long initWithByteBufferFromJNI(MappedByteBuffer modelBuffer, boolean useGPU);

  private native void deinitFromJNI(long superResolutionNativeHandle);
}
