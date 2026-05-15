package com.audicontrol.obd

data class LiveData(
    val rpm: Int = 0,
    val speedKmh: Int = 0,
    val coolantTempC: Int = 0,
    val fuelLevelPercent: Int = 0,
    val intakeTempC: Int = 0,
    val throttlePercent: Int = 0,
    val engineLoad: Int = 0,
    val voltage: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

object OBDPids {
    const val ENGINE_RPM = "010C"
    const val VEHICLE_SPEED = "010D"
    const val COOLANT_TEMP = "0105"
    const val FUEL_LEVEL = "012F"
    const val INTAKE_TEMP = "010F"
    const val THROTTLE_POS = "0111"
    const val ENGINE_LOAD = "0104"
    const val CONTROL_MODULE_VOLTAGE = "0142"

    val POLLING_PIDS = listOf(
        ENGINE_RPM,
        VEHICLE_SPEED,
        COOLANT_TEMP,
        FUEL_LEVEL,
        THROTTLE_POS,
        ENGINE_LOAD
    )
}
