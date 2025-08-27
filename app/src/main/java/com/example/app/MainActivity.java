package com.example.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // 兼容查找：优先旧ID，找不到就用新的 ndjc* ID
    Button action = findViewById(R.id.ndjcPrimary);
    Button roll   = findViewById(R.id.ndjcPrimary);
    TextView title  = findViewById(R.id.ndjcTitle);
    TextView result = findViewById(R.id.ndjcBody);

    // 如果你仍想兼容旧模板，可保留下面这段 fallback：
    Button oldAction = findViewById(R.id.btnAction);
    if (oldAction != null) action = oldAction;
    Button oldRoll = findViewById(R.id.btnRoll);
    if (oldRoll != null) roll = oldRoll;
    TextView oldTitle = findViewById(R.id.tvTitle);
    if (oldTitle != null) title = oldTitle;
    TextView oldResult = findViewById(R.id.tvResult);
    if (oldResult != null) result = oldResult;

    if (action != null && title != null) {
      action.setOnClickListener(v -> title.setText("Dice Roller clicked"));
    }
    if (roll != null && result != null) {
      roll.setOnClickListener(v -> result.setText(String.valueOf(1 + new Random().nextInt(6))));
    }
  }
}
