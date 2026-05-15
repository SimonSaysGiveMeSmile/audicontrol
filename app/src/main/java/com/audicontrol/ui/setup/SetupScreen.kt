package com.audicontrol.ui.setup

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.audicontrol.obd.ConnectionManager
import com.audicontrol.obd.ConnectionState
import com.audicontrol.obd.OBDDevice
import com.audicontrol.theme.*
import kotlinx.coroutines.launch

enum class ConnectionMode {
    CLOUD,
    BLUETOOTH_OBD
}

@Composable
fun SetupScreen(
    connectionManager: ConnectionManager,
    onCloudSelected: () -> Unit,
    onOBDConnected: () -> Unit
) {
    var selectedMode by remember { mutableStateOf<ConnectionMode?>(null) }
    val connectionStatus by connectionManager.status.collectAsState()
    val devices by connectionManager.devices.collectAsState()
    val scope = rememberCoroutineScope()

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allGranted = grants.values.all { it }
        if (allGranted) {
            connectionManager.scanForDevices()
        }
    }

    fun requestBluetoothAndScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            bluetoothPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AudiBlack)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(Modifier.height(32.dp))

        Text(
            "CONNECTION",
            style = MaterialTheme.typography.labelLarge,
            color = AudiGreyLight
        )
        Text(
            "Choose how to connect to your vehicle",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Light
        )

        Spacer(Modifier.height(16.dp))

        // Cloud option
        ConnectionOption(
            icon = Icons.Default.Cloud,
            title = "myAudi Cloud",
            description = "Remote access via Audi connect. Lock, unlock, status, honk & flash.",
            selected = selectedMode == ConnectionMode.CLOUD,
            onClick = { selectedMode = ConnectionMode.CLOUD }
        )

        // Bluetooth OBD option
        ConnectionOption(
            icon = Icons.Default.Bluetooth,
            title = "Bluetooth OBD-II",
            description = "Direct vehicle access via ELM327/OBDLink adapter. Live data, diagnostics.",
            selected = selectedMode == ConnectionMode.BLUETOOTH_OBD,
            onClick = { selectedMode = ConnectionMode.BLUETOOTH_OBD }
        )

        Spacer(Modifier.height(8.dp))

        AnimatedVisibility(visible = selectedMode == ConnectionMode.CLOUD) {
            Button(
                onClick = onCloudSelected,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AudiRed),
                shape = MaterialTheme.shapes.small
            ) {
                Text("CONTINUE WITH MYAUDI", style = MaterialTheme.typography.labelLarge)
            }
        }

        AnimatedVisibility(visible = selectedMode == ConnectionMode.BLUETOOTH_OBD) {
            BluetoothDeviceList(
                devices = devices,
                connectionState = connectionStatus.state,
                error = connectionStatus.error,
                onScan = { requestBluetoothAndScan() },
                onConnect = { device ->
                    scope.launch {
                        val success = connectionManager.connect(device)
                        if (success) onOBDConnected()
                    }
                }
            )
        }
    }
}

@Composable
private fun ConnectionOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) AudiRed else AudiGreyDark

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, MaterialTheme.shapes.small)
            .background(if (selected) AudiCardSurface else AudiDarkSurface, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) AudiRed else AudiGreyLight)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = AudiGreyLight)
        }
        if (selected) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AudiRed, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun BluetoothDeviceList(
    devices: List<OBDDevice>,
    connectionState: ConnectionState,
    error: String?,
    onScan: () -> Unit,
    onConnect: (OBDDevice) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AVAILABLE ADAPTERS", style = MaterialTheme.typography.labelSmall)
            TextButton(onClick = onScan) {
                if (connectionState == ConnectionState.SCANNING) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AudiRed)
                } else {
                    Text("SCAN", color = AudiRed)
                }
            }
        }

        if (devices.isEmpty() && connectionState != ConnectionState.SCANNING) {
            Text(
                "No OBD adapters found. Make sure your adapter is powered on and paired.",
                style = MaterialTheme.typography.bodySmall,
                color = AudiGreyLight
            )
            Button(
                onClick = onScan,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AudiRed),
                shape = MaterialTheme.shapes.small
            ) {
                Text("SCAN FOR DEVICES", style = MaterialTheme.typography.labelLarge)
            }
        }

        LazyColumn(
            modifier = Modifier.heightIn(max = 200.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(devices) { device ->
                DeviceRow(
                    device = device,
                    connecting = connectionState == ConnectionState.CONNECTING,
                    onConnect = { onConnect(device) }
                )
            }
        }

        error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = AudiRed)
        }
    }
}

@Composable
private fun DeviceRow(device: OBDDevice, connecting: Boolean, onConnect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AudiCardSurface, MaterialTheme.shapes.small)
            .clickable(enabled = !connecting, onClick = onConnect)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(device.name, style = MaterialTheme.typography.bodyMedium)
            Text(device.address, style = MaterialTheme.typography.bodySmall, color = AudiGreyLight)
        }
        if (connecting) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AudiRed)
        } else {
            Icon(Icons.Default.BluetoothConnected, contentDescription = null, tint = AudiGreyLight, modifier = Modifier.size(20.dp))
        }
    }
}
