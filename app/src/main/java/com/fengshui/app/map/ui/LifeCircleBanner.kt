package com.fengshui.app.map.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.fengshui.app.R

@Composable
fun LifeCircleBanner(
    onShowInfo: () -> Unit,
    topPanelVisible: Boolean,
    onToggleTopPanel: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(Color(0xFF263238), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.life_circle_mode_label),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.size(8.dp))
        Button(onClick = onShowInfo) { Text(stringResource(id = R.string.life_circle_show_info)) }
        Spacer(modifier = Modifier.size(8.dp))
        Button(onClick = onToggleTopPanel) {
            Text(
                text = if (topPanelVisible) {
                    stringResource(id = R.string.action_hide_top_panel)
                } else {
                    stringResource(id = R.string.action_show_top_panel)
                }
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onExit) { Text(stringResource(id = R.string.action_exit)) }
    }
}
