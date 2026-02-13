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
import androidx.compose.ui.res.stringResource
import com.fengshui.app.R

@Composable
fun RegistrationDialog(
    onDismissRequest: () -> Unit,
    onRegister: (code: String) -> Unit
) {
    val codeState = remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.registration_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = codeState.value,
                    onValueChange = { codeState.value = it },
                    label = { Text(stringResource(id = R.string.registration_code_label)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onRegister(codeState.value) }) { Text(stringResource(id = R.string.action_register)) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(id = R.string.action_cancel)) }
        }
    )
}
