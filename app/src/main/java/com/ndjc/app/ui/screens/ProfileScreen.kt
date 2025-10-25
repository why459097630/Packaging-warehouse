package com.ndjc.app.ui.screens  // com.social.app

import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*

@Composable
fun ProfileScreen() {
  Column {
    // BLOCK:USER_PROFILE_CARD
 fun UserProfileCard(user:User){ Card{ Row{ Image(painterResource(user.avatar),modifier=Modifier.size(64.dp)) Column{ Text(user.name, style=MaterialTheme.typography.h6) Text(user.bio, style=MaterialTheme.typography.body2) } } } }
// END_BLOCK

    // BLOCK:PROFILE_SECTION
    Text("Profile sections …")
    // END_BLOCK
  }
}
