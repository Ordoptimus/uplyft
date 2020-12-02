package com.nikita.uplyft;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import android.widget.ImageButton;

public class Feature extends AppCompatActivity {
    static {
        System.loadLibrary("SuperResolution");
    }

    Button superres_button;
    private static final String TAG = "SuperResolution";
    private static final String MODEL_NAME = "ESRGAN.tflite";
    private static final int LR_IMAGE_HEIGHT = 50;
    private static final int LR_IMAGE_WIDTH = 50;
    private static final int UPSCALE_FACTOR = 4;
    private static final int SR_IMAGE_HEIGHT = LR_IMAGE_HEIGHT * UPSCALE_FACTOR;
    private static final int SR_IMAGE_WIDTH = LR_IMAGE_WIDTH * UPSCALE_FACTOR;
    /*private static final String LR_IMG_1 = "lr-1.jpg";
    private static final String LR_IMG_2 = "lr-2.jpg";
    private static final String LR_IMG_3 = "lr-3.jpg";*/

    private MappedByteBuffer model;
    private long superResolutionNativeHandle = 0;
    private Bitmap selectedLRBitmap = null;
    private boolean useGPU = false;

    /*private ImageView lowResImageView1;
    private ImageView lowResImageView2;
    private ImageView lowResImageView3;
    private TextView selectedImageTextView;
    private Switch gpuSwitch;*/

    //Bitmap bitmap = (Bitmap) cacheintent.getParcelableExtra("ImageBitmap");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature);
        superres_button = findViewById(R.id.superres_button);
        //selectedLRBitmap = bitmap;

        superres_button.setOnClickListener(new View.OnClickListener() {

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

                int[] lowResRGB = new int[LR_IMAGE_HEIGHT * LR_IMAGE_WIDTH];
                selectedLRBitmap.getPixels(
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
               /*
                final LinearLayout resultLayout = findViewById(R.id.result_layout);
                final ImageView superResolutionImageView = findViewById(R.id.super_resolution_image);
                final ImageView nativelyScaledImageView = findViewById(R.id.natively_scaled_image);
                final TextView superResolutionTextView = findViewById(R.id.super_resolution_tv);
                final TextView nativelyScaledImageTextView =
                        findViewById(R.id.natively_scaled_image_tv);
                final TextView logTextView = findViewById(R.id.log_view);

                // Force refreshing the ImageView /* setting image view parameters from the superResRGB bitmap *
                superResolutionImageView.setImageDrawable(null);
                */

                Bitmap srImgBitmap =
                        Bitmap.createBitmap(
                                superResRGB, SR_IMAGE_WIDTH, SR_IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
                /*
                superResolutionImageView.setImageBitmap(srImgBitmap);
                nativelyScaledImageView.setImageBitmap(selectedLRBitmap);
                resultLayout.setVisibility(View.VISIBLE);
                logTextView.setText("Inference time: " + processingTimeMs + "ms");
                */


                Intent intSuperRes = new Intent(Feature.this, Final_Uplyfted.class);
                startActivity(intSuperRes);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        deinit();
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