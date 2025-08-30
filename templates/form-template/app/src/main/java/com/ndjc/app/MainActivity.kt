package com.ndjc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ndjc.app.data.ThemePrefs
import com.ndjc.app.ui.theme.AppTheme
import com.ndjc.app.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    private val prefs by lazy { ThemePrefs(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mode by prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val dynamic by prefs.dynamicColor.collectAsState(initial = true)
            AppTheme(themeMode = mode, dynamicColor = dynamic) {
                Surface(Modifier.fillMaxSize()) {
                    HomeScreen()
                }
            }
        }
    }
}

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "{{NDJC_HOME_TITLE}}", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { /* TODO: 你的动作逻辑 */ }) {
            Text("{{NDJC_ACTION_PRIMARY_TEXT}}")
        }
    }
}
