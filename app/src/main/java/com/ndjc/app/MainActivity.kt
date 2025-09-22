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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ndjc.app.ui.AppTheme   // 对应 app/ui/Theme.kt 的 package

// （生成器可在此追加：路由/权限/三方 SDK 的 import）

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // （如：ServiceHooks.initOnAppStart(this) / 处理 deep link 等）

    setContent {
      AppTheme {
        // 无障碍/读屏等统一注入点
        AppScaffold()
      }
    }

    // （如：埋点首帧 / 请求权限 / 新手引导）
  }
  // （生成器可追加更多生命周期/权限处理等）
}

@Composable
private fun AppScaffold() {
  // 统一使用 NavHostController（rememberNavController() 返回的就是 NavHostController）
  val nav: NavHostController = rememberNavController()

  Scaffold(
    topBar = { },
    bottomBar = { }
    // drawerContent 等也可放：
  ) { padding ->
    NavGraph(modifier = Modifier.padding(padding), nav = nav)
  }
}

/** 路由常量（生成器可在此扩展） */
private object Routes {
  const val Login    = "login"
  const val Feed     = "feed"
  const val Detail   = "detail"
  const val Search   = "search"
  const val Profile  = "profile"
  const val Settings = "settings"
  const val About    = "about"
}

@Composable
private fun NavGraph(modifier: Modifier = Modifier, nav: NavHostController) {
  NavHost(
    navController = nav,
    startDestination = Routes.Feed,
    modifier = modifier
  ) {
    composable(Routes.Login) {
      LoginScreen(onSuccess = { nav.navigate(Routes.Feed) })
    }
    composable(Routes.Feed) {
      FeedScreen(onOpen = { id -> nav.navigate("${Routes.Detail}/$id") })
    }
    composable("${Routes.Detail}/{id}") { backStack ->
      val id = backStack.arguments?.getString("id") ?: ""
      DetailScreen(id = id)
    }

    // 可选独立页（若已有独立页面可替换为调用对应 Composable）
    composable(Routes.Search)  { Text("Search") }
    composable(Routes.Profile) { Text("Profile") }
    composable(Routes.Settings){ SettingsScreen() }
    composable(Routes.About)   { AboutScreen() }

    // 生成器追加更多页面/图的锚点
  }
}

@Composable
private fun LoginScreen(onSuccess: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Button(onClick = { onSuccess() }) {
      Text(text = stringResource(R.string.ndjc_action_primary_text))
    }
  }
}

@Composable
private fun FeedScreen(onOpen: (String) -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text  = stringResource(R.string.ndjc_home_title),
      style = MaterialTheme.typography.headlineMedium
    )

    Spacer(Modifier.height(16.dp))
    Button(onClick = { onOpen("42") }) { Text("Open item #42") }

    // 列表/分页/下拉刷新/空态/加载态等扩展锚点
  }
}

@Composable
private fun DetailScreen(id: String) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text("Detail: $id")
  }
}

@Composable
private fun SettingsScreen() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(stringResource(R.string.ndjc_settings_title))
  }
}

@Composable
private fun AboutScreen() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(stringResource(R.string.ndjc_about_title))
  }
}
