package com.ndjc.simple;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.ndjc.simple.databinding.ActivityMainBinding;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonRoll.setOnClickListener(v -> {
            int n = 1 + random.nextInt(6);
            binding.textResult.setText("🎲 结果：" + n);
        });
    }
}