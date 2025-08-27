package com.example.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Button   primary = findViewById(R.id.ndjcPrimary);
    TextView title   = findViewById(R.id.ndjcTitle);
    TextView body    = findViewById(R.id.ndjcBody);

    if (primary != null) {
      primary.setOnClickListener(v -> {
        if (title != null) title.setText("Dice Roller clicked");
        if (body  != null) body.setText(String.valueOf(1 + new Random().nextInt(6)));
      });
    }
  }
}
