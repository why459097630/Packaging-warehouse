package com.example.app;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

public class MainActivity extends AppCompatActivity {
  private ArrayList<String> items = new ArrayList<>();
  private ArrayAdapter<String> adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    EditText input = findViewById(R.id.input);
    Button add = findViewById(R.id.btnAdd);
    ListView list = findViewById(R.id.list);

    String saved = getSharedPreferences("app", MODE_PRIVATE).getString("todos", "");
    if (saved != null && !saved.isEmpty()) items.addAll(Arrays.asList(saved.split("\n")));
    adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
    list.setAdapter(adapter);

    add.setOnClickListener(v -> {
      String t = input.getText().toString().trim();
      if (!t.isEmpty()) { items.add(t); adapter.notifyDataSetChanged(); input.setText(""); }
    });

    list.setOnItemLongClickListener((parent, v, pos, id) -> {
      items.remove(pos);
      adapter.notifyDataSetChanged();
      return true;
    });
  }

  @Override
  protected void onPause() {
    super.onPause();
    String joined = String.join("\n", items);
    getSharedPreferences("app", MODE_PRIVATE).edit().putString("todos", joined).apply();
  }
}