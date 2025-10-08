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
<!-- xml or kotlin snippet -->
// END_BLOCK

        // BLOCK:HOME_ACTIONS
<!-- xml or kotlin snippet -->
// END_BLOCK

        // BLOCK:HOME_BODY
<!-- xml or kotlin snippet -->
// END_BLOCK

        // BLOCK:EMPTY_STATE
<!-- xml or kotlin snippet -->
// END_BLOCK

        // BLOCK:ERROR_STATE
<!-- xml or kotlin snippet -->
// END_BLOCK

        // BLOCK:DEBUG_PANEL
<!-- xml or kotlin snippet -->
// END_BLOCK

        // BLOCK:BUILD_SUMMARY
<!-- xml or kotlin snippet -->
// END_BLOCK

        NavGraph(nav)
    }
}

@Preview
@Composable
fun PreviewApp() { NDJCApp() }
