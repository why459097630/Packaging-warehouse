package com.example.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
// NDJC:IMPORTS
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // NDJC:ONCREATE
TextView tv=findViewById(R.id.tvTitle);
Button btn=findViewById(R.id.btnAction);
btn.setOnClickListener(v -> tv.setText("DiceRoller clicked"));

    }

    // NDJC:FUNCTIONS
// default functions

}
