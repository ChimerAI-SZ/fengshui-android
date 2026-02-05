package com.fengshui.app.map.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun CompassOverlay(
    azimuthDegrees: Float,
    latitude: Double?,
    longitude: Double?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Compass circle with rotating needle
        Canvas(modifier = Modifier.size(88.dp)) {
            val radius = size.minDimension / 2f
            drawCircle(color = Color(0x33000000), radius = radius)
            drawCircle(color = Color.Black, radius = radius - 6f, style = Stroke(width = 2f))
        }

        // Rotated needle
        Canvas(modifier = Modifier.size(64.dp).rotate(azimuthDegrees)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            drawLine(color = Color.Red, start = androidx.compose.ui.geometry.Offset(cx, cy), end = androidx.compose.ui.geometry.Offset(cx, 6f), strokeWidth = 4f)
        }

        // Label: show azimuth and coords
        Text(
            text = "${"%.0f".format(azimuthDegrees)}°\n${latitude?.let { "%.4f".format(it) } ?: "--"}, ${longitude?.let { "%.4f".format(it) } ?: "--"}",
            color = Color.Black,
            modifier = Modifier.align(Alignment.BottomCenter).background(Color(0x66FFFFFF), shape = CircleShape)
        )
    }
}
