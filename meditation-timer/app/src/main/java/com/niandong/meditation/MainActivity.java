package com.niandong.meditation;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView textTimer;
    private Button buttonStart5, buttonStart10, buttonStart20;
    private CountDownTimer countDownTimer;
    private MediaPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textTimer = findViewById(R.id.textTimer);
        buttonStart5 = findViewById(R.id.buttonStart5);
        buttonStart10 = findViewById(R.id.buttonStart10);
        buttonStart20 = findViewById(R.id.buttonStart20);

        buttonStart5.setOnClickListener(v -> startTimer(5 * 60 * 1000));
        buttonStart10.setOnClickListener(v -> startTimer(10 * 60 * 1000));
        buttonStart20.setOnClickListener(v -> startTimer(20 * 60 * 1000));
    }

    private void startTimer(long millis) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        countDownTimer = new CountDownTimer(millis, 1000) {
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                textTimer.setText(String.format("%02d:%02d", minutes, seconds));
            }
            public void onFinish() {
                textTimer.setText("Done!");
                player = MediaPlayer.create(MainActivity.this, R.raw.bell);
                player.start();
            }
        }.start();
    }
}