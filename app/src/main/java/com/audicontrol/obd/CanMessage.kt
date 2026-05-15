package com.audicontrol.obd

data class CanMessage(
    val arbitrationId: Int,
    val data: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
) {
    val hex: String get() = data.joinToString("") { "%02X".format(it) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CanMessage) return false
        return arbitrationId == other.arbitrationId && data.contentEquals(other.data)
    }

    override fun hashCode(): Int = 31 * arbitrationId + data.contentHashCode()
}
