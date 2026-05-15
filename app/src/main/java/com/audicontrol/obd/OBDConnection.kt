package com.audicontrol.obd

import kotlinx.coroutines.flow.Flow

interface OBDConnection {
    val isConnected: Boolean
    suspend fun connect(address: String): Boolean
    suspend fun disconnect()
    suspend fun send(message: CanMessage)
    fun receive(): Flow<CanMessage>
}
