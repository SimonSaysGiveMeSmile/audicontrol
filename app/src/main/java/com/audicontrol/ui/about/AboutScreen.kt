package com.audicontrol.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audicontrol.BuildConfig
import com.audicontrol.theme.*

@Composable
fun AboutScreen(onLogout: (() -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AudiBlack)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("ABOUT", style = MaterialTheme.typography.labelLarge, color = AudiGreyLight)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("AudiControl", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Light)
            Text("Version 1.0  •  Build ${BuildConfig.VERSION_CODE}", style = MaterialTheme.typography.bodyMedium)
        }

        HorizontalDivider(color = AudiDivider)

        Text(
            "AudiControl is an independent, unofficial application. It is not affiliated with, endorsed by, or connected to Audi AG, Volkswagen Group, or any of their subsidiaries.\n\n" +
            "Remote features require an active Audi connect PLUS subscription on your vehicle. The app communicates with Audi's servers using the same protocol as the official myAudi app.\n\n" +
            "Audi may change or discontinue their API at any time. This app is provided as-is with no warranty.",
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp
        )

        HorizontalDivider(color = AudiDivider)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CAPABILITIES", style = MaterialTheme.typography.labelSmall)
            CapabilityRow("Lock / Unlock", available = true)
            CapabilityRow("Honk & Flash", available = true)
            CapabilityRow("Vehicle Status", available = true)
            CapabilityRow("Send Destination", available = true)
            CapabilityRow("Climate Control", available = false, note = "v2 — requires CAN Bridge")
            CapabilityRow("Infotainment", available = false, note = "v2 — requires CAN Bridge")
        }

        if (onLogout != null) {
            HorizontalDivider(color = AudiDivider)
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AudiGreyDark),
                shape = MaterialTheme.shapes.small
            ) {
                Text("SIGN OUT", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun CapabilityRow(label: String, available: Boolean, note: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (available) AudiWhite else AudiGreyMid)
        Text(
            note ?: if (available) "Available" else "Unavailable",
            style = MaterialTheme.typography.bodyMedium,
            color = if (available) AudiRed else AudiGreyMid
        )
    }
}
