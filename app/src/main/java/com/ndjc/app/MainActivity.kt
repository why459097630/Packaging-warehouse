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
// END_IF

        setContent { NDJCApp() }
    }
}

@Composable
fun NDJCApp() {
    val nav = rememberNavController()

    MaterialTheme {
        // BLOCK:HOME_HEADER
 fun HomeHeader(){ Column{ Text("Welcome to Social Circle", style=MaterialTheme.typography.h5) Spacer(modifier=Modifier.height(8.dp)) } }
// END_BLOCK

        // BLOCK:HOME_ACTIONS
 fun HomeActions(onPostClick:()->Unit){ Row{ IconButton(onClick={ onPostClick() }){ Icon(Icons.Default.Add,"Add Post") } } }
// END_BLOCK

        // BLOCK:HOME_BODY
 fun HomeBody(posts: List<Post>){ LazyColumn{ items(posts){ post-> PostCard(post) } } }
// END_BLOCK

        // BLOCK:EMPTY_STATE
 fun EmptyState(){ Column(horizontalAlignment=Alignment.CenterHorizontally){ Icon(Icons.Default.Inbox,"Empty") Text("No content yet") } }
// END_BLOCK

        // BLOCK:ERROR_STATE
 fun ErrorState(message:String,onRetry:()->Unit){ Column(horizontalAlignment=Alignment.CenterHorizontally){ Icon(Icons.Default.Error,"Error") Text(message) Button(onClick={ onRetry() }){ Text("Retry") } } }
// END_BLOCK

        // BLOCK:DEBUG_PANEL
 fun DebugPanel(info:String){ Card{ Text(info, style=MaterialTheme.typography.caption) } }
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
