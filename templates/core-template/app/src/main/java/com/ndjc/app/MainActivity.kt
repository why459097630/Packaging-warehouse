// templates/core-template/app/src/main/java/com/ndjc/app/MainActivity.kt
package com.ndjc.app

// ===== 文本锚点（示例：由生成器替换字符串资源） =====
// NDJC:HOME_TITLE
// NDJC:MAIN_BUTTON

// ===== 额外导入（块锚点） =====
// NDJC:MAIN_ACTIVITY_IMPORTS_EXTRA

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.ndjc.app.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    // ===== 状态 / Launcher / ViewModel 等（块锚点） =====
    // NDJC:STATE_AND_LAUNCHERS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // NDJC:ONCREATE_BEFORE_SET_CONTENT   // 例如：处理 Intent/DeepLink、初始化 SDK

        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // 根节点：可切换为导航容器等
                    AppRoot()
                }
            }

            // NDJC:COMPOSE_ROOT_EXTRA           // 例如：全局 SnackBarHost、DialogHost
        }

        // NDJC:ONCREATE_AFTER_SET_CONTENT    // 例如：权限请求、数据预加载
    }

    // NDJC:MAIN_ACTIVITY_EXTRA_MEMBERS       // 类级别其它函数/成员（Analytics、Service 绑定等）
}

/**
 * App 根节点：
 * - 默认展示 HomeScreen；
 * - 如启用 Navigation，可在 NDJC:NAV_HOST 注入 NavHost 并移除 HomeScreen()。
 */
@Composable
fun AppRoot() {
    // 顶部 AppBar（可选）
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.ndjc_home_title)) },
                actions = {
                    // NDJC:TOP_APP_BAR_ACTIONS
                }
            )
        }
        // NDJC:SCAFFOLD_SLOTS_EXTRA  // 例如：bottomBar = { ... }、floatingActionButton = { ... }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ====== 导航容器锚点（如使用 androidx.navigation.compose）======
            // NDJC:NAV_HOST

            // 默认单页
            HomeScreen()
        }
    }
}

/** 主页面：展示标题与主按钮，可按锚点注入更多 UI */
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

        Button(
            onClick = {
                // NDJC:MAIN_BUTTON_ONCLICK
                // 例如：打开功能页、发起网络请求、唤起原生能力等
            }
        ) {
            Text(text = stringResource(R.string.ndjc_action_primary_text))
        }

        // NDJC:HOME_SCREEN_EXTRA_BELOW_BUTTON  // 例如：副按钮、图片、列表占位等
    }
}

/** 预览（可选） */
// NDJC:COMPOSABLES_EXTRA   // 可在此追加更多可组合函数、对话框、表单等
