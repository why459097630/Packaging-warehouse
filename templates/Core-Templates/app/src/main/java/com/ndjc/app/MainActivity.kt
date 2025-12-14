package com.ndjc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

import com.ndjc.core.skeleton.*

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 路线一：让内容绘制到系统栏下方，并把状态栏设为透明
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.Transparent.toArgb()

        // 从 assets 加载 assembly 配置
        val assembly = loadAssemblyFromAssets(this, "assembly/assembly.json")

        setContent {
            NDJCAppHost(
                assembly = assembly,
                // 当前先用一个 no-op hooks 实现
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
                                nav = controller
                            )
                        }
                    )
                }
            )
        }
    }
}
