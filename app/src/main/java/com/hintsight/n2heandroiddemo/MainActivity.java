package com.hintsight.n2heandroiddemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnLogisticRegression = findViewById(R.id.btnLogisticRegression);
        Button btnFacialVerification = findViewById(R.id.btnFacialVerification);

        btnLogisticRegression.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LogisticRegressionActivity.class);
                startActivity(intent);
            }
        });

        btnFacialVerification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FacialVerificationActivity
                        .class);
                startActivity(intent);
            }
        });
    }
}