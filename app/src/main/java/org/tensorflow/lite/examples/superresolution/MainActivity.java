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

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.WorkerThread;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.common.Feature;
import com.google.firebase.database.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

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

  private MappedByteBuffer model;
  private long superResolutionNativeHandle = 0;
  private Bitmap selectedLRBitmap = null;
  private Bitmap selbitmap = null;
  private boolean useGPU = false;

  private ImageView lowResImageView1;
  private ImageView lowResImageView2;
  private ImageView lowResImageView3;
  private TextView selectedImageTextView;
  private Switch gpuSwitch;

  public static final int CAMERA_PERM_CODE = 101;
  public static final int CAMERA_REQUEST_CODE = 102;
  public static final int GALLERY_REQUEST_CODE = 105;
  ImageView selectedImage;
  ImageButton galleryBtn;
  ImageButton cameraBtn;
  String currentPhotoPath;
  Button enhancebutton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);

    selectedImage = findViewById(R.id.displayImageView);  //edit start
    galleryBtn = findViewById(R.id.galleryBtn);
    cameraBtn = findViewById(R.id.cameraBtn);
    enhancebutton = findViewById(R.id.enhancebutton);
    cameraBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        askCameraPermissions();
      }
    });

    galleryBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(gallery, GALLERY_REQUEST_CODE);
      }
    });
    /*enhancebutton.setOnClickListener(new View.OnClickListener(){
      @Override
      public void onClick(View v) {

        Toast.makeText(MainActivity.this, "Select an image", Toast.LENGTH_SHORT).show();
      }


    });  //edit end */

    selectedImage.buildDrawingCache();
    selbitmap = selectedImage.getDrawingCache();

    /* initialise button and 3 lr images */
    final Button superResolutionButton = findViewById(R.id.enhancebutton);  //edited
    //lowResImageView1 = findViewById(R.id.low_resolution_image_1);
    //lowResImageView2 = findViewById(R.id.low_resolution_image_2);
    //lowResImageView3 = findViewById(R.id.low_resolution_image_3);
    //selectedImageTextView = findViewById(R.id.chosen_image_tv);
    //gpuSwitch = findViewById(R.id.switch_use_gpu);

    //ImageView[] lowResImageViews = {lowResImageView1, lowResImageView2, lowResImageView3};

    /* create bitmaps of all 3 images */
    /*
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
    } catch (IOException e) {
      Log.e(TAG, "Failed to open an low resolution image");
    }
     */

    /* loading function to choose 1 amongst the 3 images */
    /*
    for (ImageView iv : lowResImageViews) {
      setLRImageViewListener(iv);
    }
     */

    superResolutionButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            if (selbitmap == null) {
              Toast.makeText(
                      getApplicationContext(),
                      "Please choose one low resolution image",
                      Toast.LENGTH_LONG)
                  .show();
              return;
            }

            /* GPU execution checks */
            /*
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
             */

            int[] lowResRGB = new int[LR_IMAGE_HEIGHT * LR_IMAGE_WIDTH];
            selbitmap.getPixels(                     //selectedImage's bitmap to be put
                lowResRGB, 0, LR_IMAGE_WIDTH, 0, 0, LR_IMAGE_WIDTH, LR_IMAGE_HEIGHT);

            /* calling the super-res function and storing result in superResRGB */
            final long startTime = SystemClock.uptimeMillis();
            int[] superResRGB = doSuperResolution(lowResRGB);
            final long processingTimeMs = SystemClock.uptimeMillis() - startTime;
            if (superResRGB == null) {
              showToast("Super resolution failed!");
              return;
            }

            /* the view parameters of the resulting image */
            final LinearLayout resultLayout = findViewById(R.id.result_layout);
            final ImageView superResolutionImageView = findViewById(R.id.displayImageView);
            final ImageView nativelyScaledImageView = findViewById(R.id.natively_scaled_image);
            final TextView superResolutionTextView = findViewById(R.id.super_resolution_tv);
            final TextView nativelyScaledImageTextView =
                findViewById(R.id.natively_scaled_image_tv);
            final TextView logTextView = findViewById(R.id.log_view);

            // Force refreshing the ImageView /* setting image view parameters from the superResRGB bitmap */
            superResolutionImageView.setImageDrawable(null);
            Bitmap srImgBitmap =
                Bitmap.createBitmap(
                    superResRGB, SR_IMAGE_WIDTH, SR_IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
            superResolutionImageView.setImageBitmap(srImgBitmap);
            //nativelyScaledImageView.setImageBitmap(selectedLRBitmap);
            resultLayout.setVisibility(View.VISIBLE);
            logTextView.setText("Inference time: " + processingTimeMs + "ms");
          }
        });
  }

  private void askCameraPermissions() {  //edit start
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERM_CODE);
    } else {
      dispatchTakePictureIntent();
    }

  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == CAMERA_PERM_CODE) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        dispatchTakePictureIntent();
      } else {
        Toast.makeText(this, "Camera Permission is Required to Use camera.", Toast.LENGTH_SHORT).show();
      }
    }
  }


  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == CAMERA_REQUEST_CODE) {
      if (resultCode == Activity.RESULT_OK) {
        File f = new File(currentPhotoPath);
        selectedImage.setImageURI(Uri.fromFile(f));
        Log.d("tag", "Absolute Url of Image is " + Uri.fromFile(f));

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(f);
        /*
        enhancebutton.setOnClickListener(new View.OnClickListener(){
          @Override
          public void onClick(View v) {

            Intent intEnhance = new Intent(MainActivity.this, Feature.class);
            startActivity(intEnhance);


          }


        });
         */

        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);

      }

    }

    if (requestCode == GALLERY_REQUEST_CODE) {
      if (resultCode == Activity.RESULT_OK) {
        final Uri contentUri = data.getData();

        /*
        enhancebutton.setOnClickListener(new View.OnClickListener(){
          @Override
          public void onClick(View v) {

            Intent intEnhance = new Intent(MainActivity.this, Feature.class);
            startActivity(intEnhance);

          }

        });
         */


        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "." + getFileExt(contentUri);
        Log.d("tag", "onActivityResult: Gallery Image Uri:  " + imageFileName);
        selectedImage.setImageURI(contentUri);
      }

    }

    selectedImage.buildDrawingCache();
    selbitmap = selectedImage.getDrawingCache();

  }

  private String getFileExt(Uri contentUri) {
    ContentResolver c = getContentResolver();
    MimeTypeMap mime = MimeTypeMap.getSingleton();
    return mime.getExtensionFromMimeType(c.getType(contentUri));
  }


  private File createImageFile() throws IOException {
    // Create an image file name
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String imageFileName = "JPEG_" + timeStamp + "_";
    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    //  File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    File image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",         /* suffix */
            storageDir      /* directory */
    );

    // Save a file: path for use with ACTION_VIEW intents
    currentPhotoPath = image.getAbsolutePath();
    return image;
  }


  private void dispatchTakePictureIntent() {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    // Ensure that there's a camera activity to handle the intent
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      // Create the File where the photo should go
      File photoFile = null;
      try {
        photoFile = createImageFile();
      } catch (IOException ex) {

      }
      // Continue only if the File was successfully created
      if (photoFile != null) {
        Uri photoURI = FileProvider.getUriForFile(this,
                "net.smallacademy.android.fileprovider",
                photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);

        /* creating image bitmap and storing in cache */
        selectedImage.buildDrawingCache();
        selbitmap = selectedImage.getDrawingCache();

        /*
        Intent cacheintent = new Intent(this, Feature.class);
        cacheintent.putExtra("ImageBitmap", selbitmap);*/

        /*
        ImageView imgView = (ImageView) findViewById(R.id.displayImageView);
        imgView.setImageResource(R.drawable.store_image);
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.large_icon);
        notBuilder.setLargeIcon(largeIcon);

         */
      }

    }

    selectedImage.buildDrawingCache();
    selbitmap = selectedImage.getDrawingCache();

  }  //edit ends

  @Override
  public void onDestroy() {
    super.onDestroy();
    deinit();
  }

  /* choose one image out of the three */
  /*
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
            }
            return false;
          }
        });
  }
  */

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

