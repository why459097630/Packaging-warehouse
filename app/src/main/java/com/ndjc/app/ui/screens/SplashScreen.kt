package com.ndjc.app.ui.screens  // 固定源码包名

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp

@Composable
fun SplashScreen() {
    // BLOCK:SPLASH_CONTENT
 fun SplashContent() { Column { Image(painter = painterResource(id = R.drawable.splash), contentDescription = null) Text(text = "Social Circle") } }
// END_BLOCK
}
