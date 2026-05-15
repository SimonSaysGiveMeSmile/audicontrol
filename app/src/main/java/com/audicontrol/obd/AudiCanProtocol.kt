package com.audicontrol.obd

object AudiCanProtocol {
    // UDS Service IDs relevant to Audi MIB3 / comfort module
    const val DIAGNOSTIC_SESSION_CONTROL = 0x10
    const val READ_DATA_BY_ID = 0x22
    const val WRITE_DATA_BY_ID = 0x2E
    const val ROUTINE_CONTROL = 0x31
    const val IO_CONTROL = 0x2F

    // Audi Q8 (4M) arbitration IDs
    const val GATEWAY = 0x710
    const val COMFORT_MODULE = 0x765
    const val CLIMATE_MODULE = 0x714
    const val INFOTAINMENT = 0x77A

    // Data identifiers for climate
    const val DID_AC_TEMP_SETPOINT = 0x1A00
    const val DID_AC_STATE = 0x1A01
    const val DID_SEAT_HEAT_DRIVER = 0x1A10
    const val DID_SEAT_HEAT_PASSENGER = 0x1A11

    // Data identifiers for lock
    const val DID_CENTRAL_LOCK = 0x3001
    const val DID_DOOR_STATUS = 0x3010
    const val DID_WINDOW_STATUS = 0x3020

    fun buildReadRequest(targetId: Int, did: Int): CanMessage {
        return CanMessage(
            arbitrationId = targetId,
            data = byteArrayOf(
                0x03,
                READ_DATA_BY_ID.toByte(),
                (did shr 8).toByte(),
                (did and 0xFF).toByte(),
                0x00, 0x00, 0x00, 0x00
            )
        )
    }

    fun buildWriteRequest(targetId: Int, did: Int, value: ByteArray): CanMessage {
        val payload = ByteArray(8)
        payload[0] = (3 + value.size).toByte()
        payload[1] = WRITE_DATA_BY_ID.toByte()
        payload[2] = (did shr 8).toByte()
        payload[3] = (did and 0xFF).toByte()
        value.copyInto(payload, 4, 0, minOf(value.size, 4))
        return CanMessage(arbitrationId = targetId, data = payload)
    }
}
