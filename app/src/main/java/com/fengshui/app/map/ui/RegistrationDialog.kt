package com.fengshui.app.map.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RegistrationDialog(
    onDismissRequest: () -> Unit,
    onRegister: (code: String) -> Unit
) {
    val codeState = remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("输入注册码以解锁完整功能") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = codeState.value,
                    onValueChange = { codeState.value = it },
                    label = { Text("注册码") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onRegister(codeState.value) }) { Text("注册") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("取消") }
        }
    )
}
