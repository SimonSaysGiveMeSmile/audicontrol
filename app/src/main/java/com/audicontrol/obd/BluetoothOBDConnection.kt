package com.audicontrol.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothOBDConnection : OBDConnection {

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var readJob: Job? = null

    private val _incoming = MutableSharedFlow<CanMessage>(extraBufferCapacity = 64)

    override val isConnected: Boolean
        get() = socket?.isConnected == true

    override suspend fun connect(address: String): Boolean = withContext(Dispatchers.IO) {
        try {
            disconnect()
            val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext false
            val device: BluetoothDevice = adapter.getRemoteDevice(address)
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket?.connect()
            inputStream = socket?.inputStream
            outputStream = socket?.outputStream

            initAdapter()
            startReading()
            true
        } catch (e: IOException) {
            disconnect()
            false
        }
    }

    override suspend fun disconnect() {
        readJob?.cancel()
        readJob = null
        try { inputStream?.close() } catch (_: IOException) {}
        try { outputStream?.close() } catch (_: IOException) {}
        try { socket?.close() } catch (_: IOException) {}
        socket = null
        inputStream = null
        outputStream = null
    }

    override suspend fun send(message: CanMessage) {
        withContext(Dispatchers.IO) {
            val cmd = formatATCommand(message)
            outputStream?.write(cmd.toByteArray())
            outputStream?.flush()
        }
    }

    override fun receive(): Flow<CanMessage> = _incoming.asSharedFlow()

    private suspend fun initAdapter() = withContext(Dispatchers.IO) {
        sendRaw("ATZ\r")
        delay(500)
        sendRaw("ATE0\r")
        delay(100)
        sendRaw("ATL0\r")
        delay(100)
        sendRaw("ATS0\r")
        delay(100)
        sendRaw("ATH1\r")
        delay(100)
        sendRaw("ATSP6\r") // CAN 500kbps (ISO 15765-4)
        delay(100)
        drainInput()
    }

    private fun sendRaw(cmd: String) {
        outputStream?.write(cmd.toByteArray())
        outputStream?.flush()
    }

    private fun drainInput() {
        val available = inputStream?.available() ?: 0
        if (available > 0) {
            inputStream?.read(ByteArray(available))
        }
    }

    private fun startReading() {
        readJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = StringBuilder()
            val byteBuffer = ByteArray(256)
            while (isActive && isConnected) {
                try {
                    val count = inputStream?.read(byteBuffer) ?: -1
                    if (count == -1) break
                    buffer.append(String(byteBuffer, 0, count))
                    while (buffer.contains(">")) {
                        val end = buffer.indexOf(">")
                        val frame = buffer.substring(0, end).trim()
                        buffer.delete(0, end + 1)
                        parseFrame(frame)?.let { _incoming.emit(it) }
                    }
                } catch (_: IOException) {
                    break
                }
            }
        }
    }

    private fun parseFrame(raw: String): CanMessage? {
        val cleaned = raw.replace("\\s".toRegex(), "")
        if (cleaned.length < 5 || cleaned.startsWith("NO") || cleaned.startsWith("?")) return null
        return try {
            val idHex = cleaned.substring(0, 3)
            val dataHex = cleaned.substring(3)
            val arbId = idHex.toInt(16)
            val data = dataHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            CanMessage(arbitrationId = arbId, data = data)
        } catch (_: Exception) {
            null
        }
    }

    private fun formatATCommand(message: CanMessage): String {
        val header = "%03X".format(message.arbitrationId)
        val data = message.data.joinToString("") { "%02X".format(it) }
        return "ATSH$header\r${data}\r"
    }
}
