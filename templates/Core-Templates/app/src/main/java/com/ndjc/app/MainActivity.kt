package com.ndjc.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import com.ndjc.core.skeleton.*
import com.ndjc.feature.showcase.ShowcasePushRouter

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ShowcasePushRouter.dispatchFromIntent(intent)

        // ✅ 方案 A：系统层真正沉浸（必须在 setContent 之前）
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.Transparent.toArgb()

        // ✅ 关键：Android 10+ 默认会为“可读性”在透明系统栏上加对比度遮罩（scrim）
        // 这就是你 Chat 页顶部发白/发灰的根因
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
        }

        // ✅ 你的背景偏浅：使用深色状态栏图标（否则系统可能自动做 tint/scrim）
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 从 assets 加载 assembly 配置
        val assembly = loadAssemblyFromAssets(this, "assembly/assembly.json")

        setContent {
            NDJCAppHost(
                assembly = assembly,
                hooks = object : Hooks {},
                content = { nav: NavHostController, startRoute: String, modifier: Modifier ->
                    CoreNavHost(
                        nav = nav,
                        startRoute = startRoute,
                        assembly = assembly,
                        modifier = modifier,
                        resolveScreen = { routeId, controller ->
                            ResolveCoreScreen(
                                routeId = routeId,
                                nav = controller,
                                assembly = ActiveAssembly(
                                    moduleId = assembly.modules.firstOrNull() ?: "__default__",
                                    uiPackId = assembly.uiPack ?: "__default__"
                                )
                            )
                        }
                    )
                }
            )
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ShowcasePushRouter.dispatchFromIntent(intent)
    }
}