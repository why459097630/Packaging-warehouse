package com.ndjc.app.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun PrivacyDialog(open: Boolean, onClose: () -> Unit) {
    if (!open) return
    // NDJC:BLOCK:PRIVACY_DIALOG
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Privacy") },
        text = { Text("...") },             // NDJC:PRIVACY_DIALOG_TEXT（如需在此展示具体文案）
        confirmButton = { TextButton(onClick = onClose) { Text("OK") } }
    )
}

@Composable
fun TermsDialog(open: Boolean, onClose: () -> Unit) {
    if (!open) return
    // NDJC:BLOCK:TERMS_DIALOG
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Terms") },
        text = { Text("...") },
        confirmButton = { TextButton(onClick = onClose) { Text("OK") } }
    )
}
