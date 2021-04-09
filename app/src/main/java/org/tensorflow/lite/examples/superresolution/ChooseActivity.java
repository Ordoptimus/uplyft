package org.tensorflow.lite.examples.superresolution;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ChooseActivity extends AppCompatActivity {
    Button photoediting;
    Button suprres;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose);

        photoediting=findViewById(R.id.photoediting_button);
        suprres=findViewById(R.id.supress_button);
        photoediting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoediting = new Intent(ChooseActivity.this, photoEditor.class);
                startActivity(photoediting);
            }
        });

        suprres.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent supres = new Intent(ChooseActivity.this,MainActivity.class);
                startActivity(supres);
            }
        });
    }
}