package com.example.app;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    TextView tv = findViewById(R.id.textHello);
    String marker = readAsset("build_marker.txt");
    if (tv != null) tv.setText(getString(R.string.hello_text) + " | " + marker);
  }
  private String readAsset(String path) {
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open(path)));
      StringBuilder sb = new StringBuilder(); String line;
      while ((line = br.readLine()) != null) sb.append(line);
      br.close();
      return sb.toString();
    } catch (Exception e) { return "no_marker"; }
  }
}