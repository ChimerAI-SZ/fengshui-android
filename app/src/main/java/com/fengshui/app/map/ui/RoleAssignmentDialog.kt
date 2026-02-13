package com.fengshui.app.map.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fengshui.app.data.FengShuiPoint
import com.fengshui.app.data.LifeCirclePointType
import androidx.compose.ui.res.stringResource
import com.fengshui.app.R

@Composable
fun RoleAssignmentDialog(
    origins: List<FengShuiPoint>,
    initialAssignments: Map<String, LifeCirclePointType>,
    onConfirm: (Map<String, LifeCirclePointType>) -> Unit,
    onDismiss: () -> Unit
) {
    val assignments = remember { mutableStateMapOf<String, LifeCirclePointType>() }

    if (assignments.isEmpty()) {
        assignments.putAll(initialAssignments)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.life_circle_role_assignment_title)) },
        text = {
            Column {
                origins.forEach { origin ->
                    val current = assignments[origin.id] ?: LifeCirclePointType.HOME
                    Text("${origin.name}")
                    Spacer(modifier = Modifier.size(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RoleButton(
                            label = stringResource(id = R.string.life_circle_role_home),
                            selected = current == LifeCirclePointType.HOME,
                            onClick = { assignments[origin.id] = LifeCirclePointType.HOME }
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        RoleButton(
                            label = stringResource(id = R.string.life_circle_role_work),
                            selected = current == LifeCirclePointType.WORK,
                            onClick = { assignments[origin.id] = LifeCirclePointType.WORK }
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        RoleButton(
                            label = stringResource(id = R.string.life_circle_role_entertainment),
                            selected = current == LifeCirclePointType.ENTERTAINMENT,
                            onClick = { assignments[origin.id] = LifeCirclePointType.ENTERTAINMENT }
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(assignments) }) { Text(stringResource(id = R.string.action_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.action_cancel)) }
        }
    )
}

@Composable
private fun RoleButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick) {
            Text(label)
        }
    }
}
