package com.example.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
// NDJC:IMPORTS
import android.widget.Button;
import android.widget.TextView;
import java.util.Random;

import android.widget.Button;
import android.widget.TextView;
import java.util.Random;

import android.widget.Button;
import android.widget.TextView;
import java.util.Random;

import android.widget.Button;
import android.widget.TextView;

import android.widget.Button;
import android.widget.TextView;

import android.widget.Button;
import android.widget.TextView;

import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // NDJC:ONCREATE
Button btn = findViewById(R.id.btnRoll);
TextView tv = findViewById(R.id.tvResult);
Random r = new Random();
btn.setOnClickListener(v -> tv.setText(String.valueOf(1 + r.nextInt(6))));

Button btn = findViewById(R.id.btnRoll);
TextView tv = findViewById(R.id.tvResult);
Random r = new Random();
btn.setOnClickListener(v -> tv.setText(String.valueOf(1 + r.nextInt(6))));

Button btn = findViewById(R.id.btnRoll);
TextView tv = findViewById(R.id.tvResult);
Random r = new Random();
btn.setOnClickListener(v -> tv.setText(String.valueOf(1 + r.nextInt(6))));

TextView tv=findViewById(R.id.tvTitle);
Button btn=findViewById(R.id.btnAction);
btn.setOnClickListener(v -> tv.setText("DiceRoller clicked"));

TextView tv=findViewById(R.id.tvTitle);
Button btn=findViewById(R.id.btnAction);
btn.setOnClickListener(v -> tv.setText("DiceRoller clicked"));

TextView tv=findViewById(R.id.tvTitle);
Button btn=findViewById(R.id.btnAction);
btn.setOnClickListener(v -> tv.setText("DiceRoller clicked"));

TextView tv=findViewById(R.id.tvTitle);
Button btn=findViewById(R.id.btnAction);
btn.setOnClickListener(v -> tv.setText("DiceRoller clicked"));

    }

    // NDJC:FUNCTIONS
// dice functions

// dice functions

// dice functions

// default functions

// default functions

// default functions

// default functions

}
