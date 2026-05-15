package com.audicontrol.ui.actions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.audicontrol.data.Capability
import com.audicontrol.theme.*
import com.audicontrol.ui.dashboard.DashboardViewModel

@Composable
fun ActionsScreen(viewModel: DashboardViewModel, capabilities: Set<Capability>) {
    val state by viewModel.state.collectAsState()
    var showDestDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(AudiBlack)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text("CONTROLS", style = MaterialTheme.typography.labelLarge, color = AudiGreyLight)
        Spacer(Modifier.height(16.dp))

        ActionRow(
            icon = Icons.Default.Lock,
            label = "LOCK",
            enabled = capabilities.contains(Capability.LOCK_CONTROL) && !state.actionInProgress,
            onClick = { viewModel.lock() }
        )
        ActionRow(
            icon = Icons.Default.LockOpen,
            label = "UNLOCK",
            enabled = capabilities.contains(Capability.LOCK_CONTROL) && !state.actionInProgress,
            onClick = { viewModel.unlock() }
        )
        ActionRow(
            icon = Icons.Default.Campaign,
            label = "HONK & FLASH",
            enabled = capabilities.contains(Capability.HONK_FLASH) && !state.actionInProgress,
            onClick = { viewModel.honkAndFlash() }
        )
        ActionRow(
            icon = Icons.Default.Navigation,
            label = "SEND DESTINATION",
            enabled = capabilities.contains(Capability.SEND_DESTINATION) && !state.actionInProgress,
            onClick = { showDestDialog = true }
        )

        HorizontalDivider(color = AudiDivider, modifier = Modifier.padding(vertical = 8.dp))

        // Greyed-out future capabilities
        ActionRow(
            icon = Icons.Default.AcUnit,
            label = "CLIMATE CONTROL",
            subtitle = "Requires CAN Bridge (v2)",
            enabled = false,
            onClick = {}
        )
        ActionRow(
            icon = Icons.Default.Tune,
            label = "INFOTAINMENT",
            subtitle = "Requires CAN Bridge (v2)",
            enabled = false,
            onClick = {}
        )

        if (state.actionInProgress) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(color = AudiRed, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("Sending command…", style = MaterialTheme.typography.bodyMedium)
            }
        }

        state.message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = AudiRed)
        }
    }

    if (showDestDialog) {
        SendDestinationDialog(
            onConfirm = { lat, lon, name ->
                viewModel.sendDestination(lat, lon, name)
                showDestDialog = false
            },
            onDismiss = { showDestDialog = false }
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.35f
    Row(
        Modifier
            .fillMaxWidth()
            .background(AudiCardSurface)
            .border(width = 1.dp, color = AudiDivider)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, null, tint = if (enabled) AudiRed else AudiGreyMid, modifier = Modifier.size(22.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelLarge,
                    color = if (enabled) AudiWhite else AudiGreyMid)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = AudiGreyMid)
                }
            }
        }
        if (enabled) {
            IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronRight, null, tint = AudiGreyLight)
            }
        }
    }
    Spacer(Modifier.height(2.dp))
}

@Composable
private fun SendDestinationDialog(onConfirm: (Double, Double, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AudiCardSurface,
        title = { Text("Send Destination", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(value = lat, onValueChange = { lat = it }, label = { Text("Latitude") }, singleLine = true)
                OutlinedTextField(value = lon, onValueChange = { lon = it }, label = { Text("Longitude") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val latD = lat.toDoubleOrNull() ?: return@TextButton
                    val lonD = lon.toDoubleOrNull() ?: return@TextButton
                    onConfirm(latD, lonD, name)
                }
            ) { Text("SEND", color = AudiRed) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}
