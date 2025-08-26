package com.example.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // —— 骰子功能：tvResult + btnRoll 同时存在才绑定
        TextView tvResult = findViewById(R.id.tvResult);
        Button btnRoll = findViewById(R.id.btnRoll);
        if (tvResult != null && btnRoll != null) {
            Random r = new Random();
            btnRoll.setOnClickListener(v ->
                tvResult.setText(String.valueOf(1 + r.nextInt(6)))
            );
        }

        // —— 默认功能：tvTitle + btnAction 同时存在才绑定
        TextView tvTitle = findViewById(R.id.tvTitle);
        Button btnAction = findViewById(R.id.btnAction);
        if (tvTitle != null && btnAction != null) {
            btnAction.setOnClickListener(v -> tvTitle.setText("Clicked"));
        }

        // NDJC:IMPORTS（保留锚点，生成器不会再插重复 import）
        // NDJC:ONCREATE（生成器以后在此追加事件绑定，不会再声明重复局部变量）
        // NDJC:FUNCTIONS（生成器可在类尾追加方法）
    }

    // NDJC:FUNCTIONS
}
