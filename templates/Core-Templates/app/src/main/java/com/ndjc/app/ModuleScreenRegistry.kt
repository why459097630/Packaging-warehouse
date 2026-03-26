package com.ndjc.app

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ndjc.feature.showcase.ShowcaseFeatureAssembly
import com.ndjc.feature.showcase.ui.GreenpowderShowcaseUiRenderer




// TODO：把下面这行改成你项目里真实存在的 Home 入口（任选其一）
// 方案1：如果你有 feature-showcase 的 home composable：
// import com.ndjc.feature.showcase.showcaseHomeScreen
//
// 方案2：如果你有 UI 包入口对象：
// import com.ndjc.ui.showcase.UiGreenpowderShowcaseEntry
// import com.ndjc.contract.showcase.v1.ShowcaseHomeUiState
// import com.ndjc.contract.showcase.v1.ShowcaseHomeActions

data class ActiveAssembly(
    val moduleId: String,
    val uiPackId: String
)

interface ModuleUiRenderer {
    @Composable fun Render(routeId: String, nav: NavHostController)
}

private val rendererMap: MutableMap<Pair<String, String>, ModuleUiRenderer> = mutableMapOf()

private fun register(moduleId: String, uiPackId: String, renderer: ModuleUiRenderer) {
    rendererMap[moduleId to uiPackId] = renderer
}

private fun resolve(assembly: ActiveAssembly): ModuleUiRenderer? {
    return rendererMap[assembly.moduleId to assembly.uiPackId]
        ?: rendererMap[assembly.moduleId to "__default__"]
        ?: rendererMap["__default__" to "__default__"]
}

private var registered = false

private fun ensureRegistered() {
    if (registered) return
    registered = true

    // 原有：home-basic + neumorph（先保持占位，不影响你当前调试 showcase）
    register("feature-home-basic", "ui-pack-neumorph", object : ModuleUiRenderer {
        @Composable override fun Render(routeId: String, nav: NavHostController) {
            when (routeId) {
                "home" -> {
                    Text("home-basic/neumorph home (placeholder)")
                }
                else -> Text("feature-home-basic/ui-pack-neumorph: unhandled route=$routeId")
            }
        }
    })


    // ====== 这里是关键：注册你当前正在调试的模块 + UI 包 ======
    // 下面给你两种写法，二选一，保留你用的那种即可。

// ====== feature-showcase：先跑逻辑模块内置 UI（不依赖 ui-greenpowder）======
register("feature-showcase", "__default__", object : ModuleUiRenderer {
    @Composable override fun Render(routeId: String, nav: NavHostController) {
        when (routeId) {
            "home" -> {
                // 走逻辑模块的装配入口：VM/状态机/路由分发都在 feature-showcase 内
                ShowcaseFeatureAssembly.AppRoot(
                    nav = nav,
                    ui = GreenpowderShowcaseUiRenderer
                )
            }
            else -> Text("feature-showcase/__default__: unhandled route=$routeId")
        }
    }
})



    // --- 写法 B：通过 UI 包入口渲染（需要 state/actions 来源，后续再完善）---
    /*
    register("showcase", "greenpowder", object : ModuleUiRenderer {
        @Composable override fun Render(routeId: String, nav: NavHostController) {
            when (routeId) {
                "home" -> {
                    // TODO：这里需要你从 ViewModel/Presenter 拿到真实 state/actions
                    val state = ShowcaseHomeUiState(/* ... */)
                    val actions = ShowcaseHomeActions(/* ... */)
                    UiGreenpowderShowcaseEntry.Home(state = state, actions = actions)
                }
                else -> Text("showcase+greenpowder: unhandled route=$routeId")
            }
        }
    })
    */
}

@Composable
fun ResolveCoreScreen(
    routeId: String,
    nav: NavHostController,
    assembly: ActiveAssembly
) {
    ensureRegistered()

    when (routeId) {
        "about" -> LocalAboutScreen()
        else -> {
            val renderer = resolve(assembly)
            if (renderer == null) {
                Text("NDJC: Missing renderer. module=${assembly.moduleId}, uiPack=${assembly.uiPackId}, route=$routeId")
            }
            else {
                renderer.Render(routeId = routeId, nav = nav)
            }
        }
    }
}

@Composable
private fun LocalAboutScreen() {
    Text("NDJC demo about (placeholder)")
}
