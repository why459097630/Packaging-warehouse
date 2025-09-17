package com.ndjc.app

// ===== Imports =====
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ndjc.app.ui.theme.AppTheme

// NDJC:MAIN_ACTIVITY_IMPORTS_EXTRA
// （生成器可在此追加：深链、埋点、路由、第三方 SDK 等所需 import）

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // NDJC:ONCREATE_BEFORE_SUPER
        super.onCreate(savedInstanceState)

        // NDJC:ONCREATE_BEFORE_SET_CONTENT
        // （如：ServiceHooks.initOnAppStart(this) / 处理 intent deep link 等）

        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen()
                }
            }
        }

        // NDJC:ONCREATE_AFTER_SET_CONTENT
        // （如：曝光埋点 / 请求权限弹窗 / 首屏引导 / 首次同步等）
    }

    // NDJC:MAIN_ACTIVITY_EXTRA_FUNCS
    // （生成器可追加更多生命周期/回调/权限处理等）
}

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.ndjc_home_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            // NDJC:PRIMARY_BUTTON_ACTION
            // （生成器可注入：导航到某页面/打开 WebView/触发登录等）
        }) {
            Text(text = stringResource(R.string.ndjc_action_primary_text))
        }
    }
}
