package com.audicontrol.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.audicontrol.theme.*

@Composable
fun VehicleSilhouette(
    locked: Boolean,
    doorsOpen: List<String>,
    windowsOpen: List<String>,
    actionInProgress: Boolean,
    modifier: Modifier = Modifier
) {
    val lockColor by animateColorAsState(
        targetValue = if (locked) AudiGreyLight else AudiRed,
        animationSpec = tween(400),
        label = "lockColor"
    )

    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.width(180.dp).height(260.dp)) {
            val w = size.width
            val h = size.height
            val bodyColor = AudiGreyDark
            val outlineColor = AudiGreyLight

            // SUV body outline (top-down view)
            val body = Path().apply {
                moveTo(w * 0.25f, h * 0.1f)
                // front curve
                cubicTo(w * 0.3f, h * 0.05f, w * 0.7f, h * 0.05f, w * 0.75f, h * 0.1f)
                // right side
                lineTo(w * 0.78f, h * 0.35f)
                lineTo(w * 0.8f, h * 0.5f)
                lineTo(w * 0.78f, h * 0.7f)
                // rear curve
                cubicTo(w * 0.76f, h * 0.88f, w * 0.24f, h * 0.88f, w * 0.22f, h * 0.7f)
                // left side
                lineTo(w * 0.2f, h * 0.5f)
                lineTo(w * 0.22f, h * 0.35f)
                close()
            }
            drawPath(body, color = bodyColor)
            drawPath(body, color = outlineColor, style = Stroke(width = 1.5f))

            // Windshield
            drawRoundRect(
                color = AudiCardSurface,
                topLeft = Offset(w * 0.32f, h * 0.15f),
                size = Size(w * 0.36f, h * 0.12f),
                cornerRadius = CornerRadius(4f)
            )

            // Rear window
            drawRoundRect(
                color = AudiCardSurface,
                topLeft = Offset(w * 0.32f, h * 0.72f),
                size = Size(w * 0.36f, h * 0.1f),
                cornerRadius = CornerRadius(4f)
            )

            // Doors (highlight open ones in red)
            val doorPositions = mapOf(
                "front_left" to Offset(w * 0.18f, h * 0.3f),
                "front_right" to Offset(w * 0.78f, h * 0.3f),
                "rear_left" to Offset(w * 0.18f, h * 0.55f),
                "rear_right" to Offset(w * 0.78f, h * 0.55f)
            )
            doorPositions.forEach { (door, pos) ->
                val isOpen = doorsOpen.any { it.lowercase().contains(door.replace("_", "")) || it.lowercase().contains(door) }
                val color = if (isOpen) AudiRed else AudiGreyDark
                drawRoundRect(
                    color = color,
                    topLeft = pos,
                    size = Size(w * 0.04f, h * 0.12f),
                    cornerRadius = CornerRadius(2f)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Lock status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val iconAlpha = if (actionInProgress) pulseAlpha else 1f
            Icon(
                imageVector = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = null,
                tint = lockColor.copy(alpha = iconAlpha),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = if (locked) "LOCKED" else "UNLOCKED",
                style = MaterialTheme.typography.labelLarge,
                color = lockColor
            )
        }
    }
}
