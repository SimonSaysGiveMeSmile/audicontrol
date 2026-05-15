package com.audicontrol.data

import kotlinx.serialization.Serializable

@Serializable
data class Vehicle(
    val vin: String,
    val model: String,
    val year: Int,
    val nickname: String? = null
)

@Serializable
data class VehicleStatus(
    val vin: String,
    val locked: Boolean,
    val fuelLevelPercent: Int,
    val rangeKm: Int,
    val odometerKm: Int,
    val doorsOpen: List<String>,
    val windowsOpen: List<String>,
    val latitude: Double?,
    val longitude: Double?,
    val lastUpdated: Long
)

enum class Capability {
    STATUS,
    LOCK_CONTROL,
    HONK_FLASH,
    SEND_DESTINATION,
    CLIMATE_CONTROL,
    INFOTAINMENT_CONTROL
}

sealed class VehicleResult<out T> {
    data class Success<T>(val data: T) : VehicleResult<T>()
    data class Error(val message: String) : VehicleResult<Nothing>()
}
