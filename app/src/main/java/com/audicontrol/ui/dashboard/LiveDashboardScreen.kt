package com.audicontrol.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audicontrol.obd.LiveData
import com.audicontrol.obd.LiveDataStream
import com.audicontrol.theme.*
import com.audicontrol.ui.components.RingGauge

@Composable
fun LiveDashboardScreen(liveDataStream: LiveDataStream) {
    val data by liveDataStream.liveData.collectAsState()
    val streaming by liveDataStream.isStreaming.collectAsState()

    DisposableEffect(Unit) {
        liveDataStream.start()
        onDispose { liveDataStream.stop() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AudiBlack)
            .verticalScroll(rememberScrollState())
    ) {
        // Live indicator
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (streaming) AudiRedDim else AudiGreyDark)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (streaming) {
                PulsingDot()
            }
            Text(
                if (streaming) "LIVE" else "DISCONNECTED",
                style = MaterialTheme.typography.labelSmall,
                color = AudiWhite
            )
        }

        Spacer(Modifier.height(16.dp))

        // Speed + RPM hero section
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "${data.speedKmh}",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                fontWeight = FontWeight.Thin,
                color = AudiWhite
            )
            Text("km/h", style = MaterialTheme.typography.labelMedium, color = AudiGreyLight)

            Spacer(Modifier.height(8.dp))

            Text(
                "${data.rpm} RPM",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Light,
                color = AudiRed
            )
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = AudiDivider)

        // Ring gauges row
        Row(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RingGauge(
                value = data.coolantTempC.toFloat(),
                maxValue = 130f,
                label = "COOLANT",
                displayValue = "${data.coolantTempC}°C"
            )
            RingGauge(
                value = data.fuelLevelPercent.toFloat(),
                maxValue = 100f,
                label = "FUEL",
                displayValue = "${data.fuelLevelPercent}%"
            )
            RingGauge(
                value = data.throttlePercent.toFloat(),
                maxValue = 100f,
                label = "THROTTLE",
                displayValue = "${data.throttlePercent}%"
            )
        }

        HorizontalDivider(color = AudiDivider)

        // Detail rows
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            LiveDetailRow("ENGINE LOAD", "${data.engineLoad}%")
            LiveDetailRow("INTAKE TEMP", "${data.intakeTempC}°C")
            LiveDetailRow("VOLTAGE", "%.1f V".format(data.voltage))
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LiveDetailRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(AudiCardSurface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = AudiWhite)
    }
}

@Composable
private fun PulsingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    Box(
        Modifier
            .size(8.dp)
            .background(
                AudiRed.copy(alpha = alpha),
                shape = MaterialTheme.shapes.small
            )
    )
}
