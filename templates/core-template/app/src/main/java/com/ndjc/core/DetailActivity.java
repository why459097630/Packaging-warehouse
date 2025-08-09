package com.ndjc.core;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.ndjc.core.databinding.ActivityDetailBinding;

public class DetailActivity extends AppCompatActivity {
    private ActivityDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.textInfo.setText("这是详情页。你可以在这里展示图文内容。");
    }
}