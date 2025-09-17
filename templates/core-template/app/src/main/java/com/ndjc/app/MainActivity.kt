package com.ndjc.app

// ===== Imports =====
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ndjc.app.ui.AppTheme

// NDJC:MAIN_ACTIVITY_IMPORTS_EXTRA
// （生成器可在此追加：路由/权限/三方 SDK 的 import）

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // NDJC:ONCREATE_BEFORE_SUPER
        super.onCreate(savedInstanceState)

        // NDJC:ONCREATE_BEFORE_SET_CONTENT
        // （如：ServiceHooks.initOnAppStart(this) / 处理 deep link 等）

        setContent {
            AppTheme {
                // 无障碍/读屏等统一注入点
                // NDJC:BLOCK:ACCESSIBILITY

                AppScaffold()
            }
        }

        // NDJC:ONCREATE_AFTER_SET_CONTENT
        // （如：埋点首帧 / 请求权限 / 新手引导）
    }

    // NDJC:MAIN_ACTIVITY_EXTRA_FUNCS
    // （生成器可追加更多生命周期/权限处理等）
}

@Composable
private fun AppScaffold() {
    val nav = rememberNavController()

    Scaffold(
        topBar = { /* NDJC:BLOCK:NAV_TOPBAR */ },
        bottomBar = { /* NDJC:BLOCK:NAV_BOTTOM */ }
    ) { padding ->
        NavGraph(modifier = Modifier.padding(padding), nav = nav)
    }
}

/** 路由常量（可由生成器扩展） */
private object Routes {
    const val Login    = "login"      // NDJC:BLOCK:ROUTER_TABLE
    const val Feed     = "feed"       // NDJC:BLOCK:ROUTER_TABLE
    const val Detail   = "detail"     // NDJC:BLOCK:ROUTER_TABLE
    const val Search   = "search"     // NDJC:BLOCK:ROUTER_TABLE
    const val Profile  = "profile"    // NDJC:BLOCK:ROUTER_TABLE
    const val Settings = "settings"   // NDJC:BLOCK:ROUTER_TABLE
}

@Composable
private fun NavGraph(modifier: Modifier = Modifier, nav: NavController) {
    NavHost(
        navController = nav,
        startDestination = Routes.Feed,      // NDJC:START_ROUTE（TEXT）
        modifier = modifier
    ) {
        composable(Routes.Login) {
            // NDJC:BLOCK:FEATURE_LOGIN
            LoginScreen(onSuccess = { nav.navigate(Routes.Feed) })
        }
        composable(Routes.Feed) {
            // NDJC:BLOCK:FEATURE_FEED
            FeedScreen(onOpen = { id -> nav.navigate("${Routes.Detail}/$id") })
        }
        composable("${Routes.Detail}/{id}") { backStack ->
            // NDJC:BLOCK:ROUTE_ARGS
            val id = backStack.arguments?.getString("id") ?: ""
            // NDJC:BLOCK:FEATURE_DETAIL
            DetailScreen(id = id)
        }
        composable(Routes.Search)  { /* NDJC:BLOCK:FEATURE_SEARCH       */ }
        composable(Routes.Profile) { /* NDJC:BLOCK:FEATURE_USER_PROFILE */ }
        composable(Routes.Settings){ /* NDJC:BLOCK:SETTINGS_PAGE        */ }

        // 这里是生成器追加更多页面/图的锚点
        // NDJC:BLOCK:NAV_GRAPH
        // NDJC:BLOCK:NAV_DRAWER
    }
}

@Composable
private fun LoginScreen(onSuccess: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            // NDJC:BLOCK:FEATURE_LOGIN（接入真实登录后替换）
            onSuccess()
        }) { Text(text = stringResource(R.string.ndjc_action_primary_text)) }
    }
}

@Composable
private fun FeedScreen(onOpen: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.ndjc_home_title),
            style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))
        Button(onClick = { onOpen("42") }) { Text("Open item #42") }
        // 列表/分页/下拉刷新/空态/加载态等
        // NDJC:BLOCK:FEATURE_LIST
        // NDJC:BLOCK:PULL_TO_REFRESH
        // NDJC:BLOCK:EMPTY_STATE
        // NDJC:BLOCK:LOADING_STATE
    }
}

@Composable
private fun DetailScreen(id: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Detail: $id")
        // NDJC:BLOCK:FEATURE_DETAIL
    }
}
