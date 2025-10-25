package com.ndjc.app.ui.screens  // 固定源码包名

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp

@Composable
fun SplashScreen() {
    // BLOCK:SPLASH_CONTENT
 fun SplashScreen(){ Box(modifier=Modifier.fillMaxSize(),contentAlignment=Alignment.Center){ Image(painterResource(R.drawable.splash),contentDescription=null) } }
// END_BLOCK
}
