package com.ndjc.app.feature.post

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun PostEditorScreen(onBack: () -> Unit) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("New Post") }) }
    ) { inner ->
        Column(Modifier.padding(inner).padding(16.dp)) {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(), minLines = 4,
                placeholder = { Text("Say something…") }
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = { /* todo: persist */ onBack() }) { Text("Publish") }
        }
    }
}
