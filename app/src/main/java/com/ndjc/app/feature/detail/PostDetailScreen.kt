package com.ndjc.app.feature.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ndjc.app.data.SeedRepository

@Composable
fun PostDetailScreen(id: String, onBack: () -> Unit) {
    val post = remember(id) { SeedRepository.postById(id) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(post?.author ?: "Detail") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { inner ->
        Column(Modifier.padding(inner).padding(16.dp)) {
            Text(text = post?.content ?: "N/A", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(12.dp))
            Text("Comments: ${post?.comments?.size ?: 0}")
        }
    }
}
