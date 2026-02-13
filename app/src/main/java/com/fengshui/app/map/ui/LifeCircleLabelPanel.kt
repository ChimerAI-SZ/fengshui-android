package com.fengshui.app.map.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.fengshui.app.R

@Composable
fun LifeCircleLabelPanel(
    homeLabels: List<String>,
    workLabels: List<String>,
    entertainmentLabels: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            LifeCircleLabelSection(stringResource(id = R.string.life_circle_role_home), homeLabels)
            Spacer(modifier = Modifier.size(6.dp))
            LifeCircleLabelSection(stringResource(id = R.string.life_circle_role_work), workLabels)
            Spacer(modifier = Modifier.size(6.dp))
            LifeCircleLabelSection(stringResource(id = R.string.life_circle_role_entertainment_places), entertainmentLabels)
        }
    }
}

@Composable
private fun LifeCircleLabelSection(title: String, labels: List<String>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF263238)
        )
        Spacer(modifier = Modifier.size(4.dp))
        if (labels.isEmpty()) {
            Text(stringResource(id = R.string.life_circle_no_connections), fontSize = 11.sp, color = Color.Gray)
        } else {
            labels.forEach { label ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(6.dp))
                        .padding(6.dp)
                ) {
                    Text(label, fontSize = 11.sp, color = Color(0xFF333333))
                }
                Spacer(modifier = Modifier.size(4.dp))
            }
        }
    }
}
