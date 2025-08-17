fetch('/api/push-to-github', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    packageName: 'com.example.meditationtimer',
    java: {
      'MainActivity.java':
        'package com.example.meditationtimer;\n' +
        'import android.os.Bundle;\n' +
        'import android.widget.TextView;\n' +
        'import androidx.appcompat.app.AppCompatActivity;\n' +
        'public class MainActivity extends AppCompatActivity {\n' +
        '  @Override protected void onCreate(Bundle s){\n' +
        '    super.onCreate(s);\n' +
        '    setContentView(R.layout.activity_main);\n' +
        '    TextView tv=findViewById(R.id.textHello);\n' +
        '    if(tv!=null) tv.setText("Hello from API code");\n' +
        '  }\n' +
        '}\n'
    },
    resLayout: { // 注意是 resLayout，L 大写
      'activity_main.xml':
        '<?xml version="1.0" encoding="utf-8"?>\n' +
        '<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"\n' +
        '  android:orientation="vertical" android:gravity="center"\n' +
        '  android:layout_width="match_parent" android:layout_height="match_parent">\n' +
        '  <TextView android:id="@+id/textHello" android:text="Placeholder"\n' +
        '    android:layout_width="wrap_content" android:layout_height="wrap_content"/>\n' +
        '</LinearLayout>\n'
    },
    resValues: {
      'strings.xml': '<resources>\n  <string name="app_name">Niandong Demo</string>\n</resources>\n'
    }
  })
}).then(r => r.json()).then(console.log).catch(console.error);
