package com.ndjc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.compose.material3.ExperimentalMaterial3Api

import com.ndjc.core.skeleton.*
import com.ndjc.ui.neu.theme.NDJCTheme as UiTheme
import com.ndjc.ui.neu.theme.ThemeMode as UiThemeMode
import com.ndjc.ui.neu.theme.Density as UiDensity
import com.ndjc.ui.neu.components.NDJCTabBar as UiTabBar
import com.ndjc.ui.neu.components.NDJCTopAppBar as UiTopAppBar

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val assembly =
            loadAssemblyFromAssets(this, "assembly/assembly.json")

        setContent {
            UiTheme(
                mode = UiThemeMode.Light,
            ) {
                NDJCAppHost(
                    assembly = assembly,
                    // 最简单的 hooks 实现，先全部 no-op
                    hooks = object : Hooks {},
                    header = { UiTopAppBar(title = "NDJC Skeleton") },
                    tabBar = { navigator, currentRouteId ->
                        val routes = remember(assembly) {
                            resolveTabRoutesFromModules(
                                modules = assembly.modules,
                                // 新签名要求的 fallback，先给空列表兜底
                                fallback = emptyList()
                            )
                        }

                        UiTabBar(
                            items = routes,
                            selectedId = currentRouteId,
                            onClick = { id -> navigator.navigate(id) }
                        )
                    },
                    content = { nav: NavHostController, startRoute: String, modifier: Modifier ->
                        CoreNavHost(
                            nav = nav,
                            startRoute = startRoute,
                            assembly = assembly,
                            modifier = modifier,
                            // ⭐ 这里把之前加在 Routes.kt 里的 resolveScreen 补上
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
}
