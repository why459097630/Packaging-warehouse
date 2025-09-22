package com.ndjc.app // NDJC:PACKAGE_NAME

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.ndjc.app.navigation.NavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // HOOK:AFTER_INSTALL  // 首次启动时占位（生成器可注入一次性逻辑）
        setContent { NDJCApp() }
    }
}

@Composable
fun NDJCApp() {
    val nav = rememberNavController()
    MaterialTheme {
        // BLOCK:DEBUG_PANEL
        // 可注入悬浮调试开关/环境信息
        // END_BLOCK

        // BLOCK:BUILD_SUMMARY
        // 可注入 BuildConfig.NDJC_RUN_ID / 构建摘要的小条
        // END_BLOCK

        NavGraph(nav)
    }
}

@Preview @Composable
fun PreviewApp() { NDJCApp() }
