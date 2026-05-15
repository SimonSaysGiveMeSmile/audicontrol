package com.audicontrol.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class OBDDevice(
    val name: String,
    val address: String,
    val paired: Boolean
)

data class ConnectionStatus(
    val state: ConnectionState = ConnectionState.DISCONNECTED,
    val device: OBDDevice? = null,
    val error: String? = null
)

@SuppressLint("MissingPermission")
class ConnectionManager(private val context: Context) {

    private val _status = MutableStateFlow(ConnectionStatus())
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _devices = MutableStateFlow<List<OBDDevice>>(emptyList())
    val devices: StateFlow<List<OBDDevice>> = _devices.asStateFlow()

    private val connection = BluetoothOBDConnection()
    private var scanJob: Job? = null

    val obdConnection: OBDConnection get() = connection

    fun scanForDevices() {
        scanJob?.cancel()
        _status.value = ConnectionStatus(state = ConnectionState.SCANNING)
        scanJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                if (adapter == null || !adapter.isEnabled) {
                    _status.value = ConnectionStatus(
                        state = ConnectionState.ERROR,
                        error = "Bluetooth is not available or disabled"
                    )
                    return@launch
                }

                val paired = adapter.bondedDevices?.mapNotNull { device ->
                    if (isOBDDevice(device)) {
                        OBDDevice(
                            name = device.name ?: "Unknown",
                            address = device.address,
                            paired = true
                        )
                    } else null
                } ?: emptyList()

                _devices.value = paired
                _status.value = ConnectionStatus(state = ConnectionState.DISCONNECTED)
            } catch (e: SecurityException) {
                _status.value = ConnectionStatus(
                    state = ConnectionState.ERROR,
                    error = "Bluetooth permission denied"
                )
            }
        }
    }

    suspend fun connect(device: OBDDevice): Boolean {
        _status.value = ConnectionStatus(state = ConnectionState.CONNECTING, device = device)
        val success = connection.connect(device.address)
        _status.value = if (success) {
            ConnectionStatus(state = ConnectionState.CONNECTED, device = device)
        } else {
            ConnectionStatus(
                state = ConnectionState.ERROR,
                device = device,
                error = "Failed to connect to ${device.name}"
            )
        }
        return success
    }

    suspend fun disconnect() {
        connection.disconnect()
        _status.value = ConnectionStatus(state = ConnectionState.DISCONNECTED)
    }

    private fun isOBDDevice(device: BluetoothDevice): Boolean {
        val name = device.name?.uppercase() ?: return false
        return name.contains("OBD") ||
                name.contains("ELM") ||
                name.contains("VLINK") ||
                name.contains("VEEPEAK") ||
                name.contains("KONNWEI") ||
                name.contains("SCAN") ||
                name.contains("CARISTA") ||
                name.contains("OBDLINK")
    }
}
