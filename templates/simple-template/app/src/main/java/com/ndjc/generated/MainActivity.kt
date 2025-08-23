package com.ndjc.generated

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ndjc.generated.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 读取我们注入的 API 配置（strings.xml 由 CI 注入）
        val base   = getString(R.string.api_base)
        val secret = getString(R.string.api_secret)

        // 仅示例：把标题设为 app_name，或把 base/secret 打到日志、页面上
        title = getString(R.string.app_name)
    }
}
