package com.ndjc.app  // 固定源码包名

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.ndjc.app.navigation.NavGraph

class MainActivity : ComponentActivity() {

    // HOOK PERMISSIONS:ON_REQUEST
    // 在这里集中放置运行时权限的 launcher / 回调等实现
    // END_HOOK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // HOOK AFTER_INSTALL:HOOK
        // 这里可放“首次安装后”的一次性逻辑，比如打点/引导页触发
        // END_HOOK

        // IF:AFTER_INSTALL
        // 若需要在代码侧（而非 CI 脚本）执行“安装后”动作，可在此注入
        // END_IF

        setContent { NDJCApp() }
    }
}

@Composable
fun NDJCApp() {
    val nav = rememberNavController()

    MaterialTheme {
        // BLOCK:HOME_HEADER
 fun HomeHeader() { Row { Text(text = "Welcome") } }
// END_BLOCK

        // BLOCK:HOME_ACTIONS
 fun HomeActions() { Row { Button(onClick = {}) { Text("New Post") } Button(onClick = {}) { Text("Search") } } }
// END_BLOCK

        // BLOCK:HOME_BODY
 fun HomeBody() { Column { Text(text = "Latest posts") Button(onClick = {}) { Text("Refresh") } } }
// END_BLOCK

        // BLOCK:EMPTY_STATE
        // END_BLOCK

        // BLOCK:ERROR_STATE
        // END_BLOCK

        // BLOCK:DEBUG_PANEL
        // 可注入：调试开关/环境变量（如网络 Host、用户态等）
        // END_BLOCK

        // BLOCK:BUILD_SUMMARY
        // 可注入 BuildConfig/NDJC_RUN_ID / 构建摘要/变更
        // END_BLOCK

        NavGraph(nav)
    }
}

@Preview
@Composable
fun PreviewApp() { NDJCApp() }
