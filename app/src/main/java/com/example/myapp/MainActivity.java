package com.example.myapp;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
  private CountDownTimer timer;
  private boolean running = false;
  private long totalMillis = 10L * 1000L;
  private long leftMillis = totalMillis;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    TextView tv = findViewById(R.id.tvTime);
    Button start = findViewById(R.id.btnStart);
    Button stop  = findViewById(R.id.btnStop);
    updateText(tv);

    start.setOnClickListener(v -> {
      if (running) return;
      running = true;
      timer = new CountDownTimer(leftMillis, 1000) {
        public void onTick(long ms) { leftMillis = ms; updateText(tv); }
        public void onFinish() { running = false; leftMillis = totalMillis; updateText(tv); }
      }.start();
    });

    stop.setOnClickListener(v -> {
      if (timer != null) timer.cancel();
      running = false;
    });
  }

  private void updateText(TextView tv) {
    long sec = leftMillis / 1000;
    tv.setText(sec + " s");
  }
}