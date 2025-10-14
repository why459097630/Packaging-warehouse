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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // HOOK:AFTER_INSTALL
        // END_HOOK

        setContent { NDJCApp() }
    }
}

@Composable
fun NDJCApp() {
    val nav = rememberNavController()

    MaterialTheme {
        // BLOCK:HOME_HEADER
 fun HomeHeader() { Text("菜单") }
// END_BLOCK

        // BLOCK:HOME_ACTIONS
        // END_BLOCK

        // BLOCK:HOME_BODY
 fun HomeBody() { LazyColumn { items = listOf( "菜品1", "菜品2", "菜品3") { item -> Text(item) } } }
// END_BLOCK

        // BLOCK:EMPTY_STATE
        // END_BLOCK

        // BLOCK:ERROR_STATE
        // END_BLOCK

        // BLOCK:DEBUG_PANEL
val debugPanel = DebugPanel()
// END_BLOCK

        // BLOCK:BUILD_SUMMARY
val buildSummary = BuildSummary()
// END_BLOCK

        NavGraph(nav)
    }
}

@Preview
@Composable
fun PreviewApp() { NDJCApp() }
