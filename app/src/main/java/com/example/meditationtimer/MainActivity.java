package com.example.meditationtimer;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
  @Override protected void onCreate(Bundle s){
    super.onCreate(s);
    setContentView(R.layout.activity_main);
    TextView tv = findViewById(R.id.textHello);
    if (tv != null) tv.setText("Hello from API code");
  }
}
