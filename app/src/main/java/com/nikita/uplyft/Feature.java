package com.nikita.uplyft;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Feature extends AppCompatActivity {
    Button superres_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature);
        superres_button = findViewById(R.id.superres_button);

        superres_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intSuperRes = new Intent(Feature.this, Final_Uplyfted.class);
                startActivity(intSuperRes);


            }
        });
    }
}