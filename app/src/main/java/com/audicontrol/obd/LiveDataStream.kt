package com.audicontrol.obd

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException

class LiveDataStream(private val connection: OBDConnection) {

    private val _liveData = MutableStateFlow(LiveData())
    val liveData: StateFlow<LiveData> = _liveData.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private var pollingJob: Job? = null

    fun start() {
        if (pollingJob?.isActive == true) return
        _isStreaming.value = true
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && connection.isConnected) {
                for (pid in OBDPids.POLLING_PIDS) {
                    if (!isActive || !connection.isConnected) break
                    try {
                        val response = queryPid(pid)
                        response?.let { updateLiveData(pid, it) }
                    } catch (_: IOException) {
                        break
                    }
                    delay(50)
                }
                delay(100)
            }
            _isStreaming.value = false
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
        _isStreaming.value = false
    }

    private suspend fun queryPid(pid: String): ByteArray? {
        val cmd = CanMessage(
            arbitrationId = 0x7DF,
            data = pidToBytes(pid)
        )
        connection.send(cmd)
        return withTimeoutOrNull(200) {
            connection.receive().firstOrNull()?.data
        }
    }

    private fun pidToBytes(pid: String): ByteArray {
        val bytes = pid.chunked(2).map { it.toInt(16).toByte() }
        val frame = ByteArray(8)
        frame[0] = bytes.size.toByte()
        bytes.forEachIndexed { i, b -> frame[i + 1] = b }
        return frame
    }

    private fun updateLiveData(pid: String, response: ByteArray) {
        if (response.size < 3) return
        val a = response[2].toInt() and 0xFF
        val b = if (response.size > 3) response[3].toInt() and 0xFF else 0

        val current = _liveData.value
        _liveData.value = when (pid) {
            OBDPids.ENGINE_RPM -> current.copy(rpm = ((a * 256) + b) / 4)
            OBDPids.VEHICLE_SPEED -> current.copy(speedKmh = a)
            OBDPids.COOLANT_TEMP -> current.copy(coolantTempC = a - 40)
            OBDPids.FUEL_LEVEL -> current.copy(fuelLevelPercent = (a * 100) / 255)
            OBDPids.INTAKE_TEMP -> current.copy(intakeTempC = a - 40)
            OBDPids.THROTTLE_POS -> current.copy(throttlePercent = (a * 100) / 255)
            OBDPids.ENGINE_LOAD -> current.copy(engineLoad = (a * 100) / 255)
            OBDPids.CONTROL_MODULE_VOLTAGE -> current.copy(voltage = ((a * 256) + b) / 1000f)
            else -> current
        }.copy(timestamp = System.currentTimeMillis())
    }
}
