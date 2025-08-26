package com.example.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    // NDJC:IMPORTS
    // 这里故意不再插重复 import；代码生成器也改为不用本地变量，避免重复定义。

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // NDJC:ONCREATE
((android.widget.Button) findViewById(R.id.btnRoll)).setOnClickListener(v -> ((android.widget.TextView) findViewById(R.id.tvResult)).setText(String.valueOf(1 + new java.util.Random().nextInt(6))));

        // —— Dice（tvResult / btnRoll）：仅当两者都存在时才绑定监听，避免找不到 id 导致 NPE。
        TextView tvResult = findViewById(R.id.tvResult);
        Button btnRoll = findViewById(R.id.btnRoll);
        if (tvResult != null && btnRoll != null) {
            btnRoll.setOnClickListener(v ->
                tvResult.setText(String.valueOf(1 + new java.util.Random().nextInt(6)))
            );
        }

        // —— Default（tvTitle / btnAction）：仅当两者都存在时才绑定监听。
        TextView tvTitle = findViewById(R.id.tvTitle);
        Button btnAction = findViewById(R.id.btnAction);
        if (tvTitle != null && btnAction != null) {
            btnAction.setOnClickListener(v -> tvTitle.setText("Clicked"));
        }

        // 说明：
        // 1) 不再使用局部变量的重复声明（btn/tv/r 等），多次注入也不会编译失败。
        // 2) Random 使用全限定类名 new java.util.Random()，无需新增 import。
    }

    // NDJC:FUNCTIONS
// dice functions

    // 生成器可在此处继续插入辅助方法（不会影响 onCreate 的局部变量作用域）。
}
