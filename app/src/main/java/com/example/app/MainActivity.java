package com.example.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // NDJC:ONCREATE
((android.widget.Button) findViewById(R.id.btnRoll)).setOnClickListener(v -> ((android.widget.TextView) findViewById(R.id.tvResult)).setText(String.valueOf(1 + new java.util.Random().nextInt(6))));

        // 生成器会在这里插入事件绑定（如为按钮设置 onClickListener）
    }

    // NDJC:FUNCTIONS
// dice functions

    // 生成器需要时会在这里追加辅助方法
}
