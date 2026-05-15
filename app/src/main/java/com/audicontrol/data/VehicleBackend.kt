package com.audicontrol.data

interface VehicleBackend {
    val capabilities: Set<Capability>
    suspend fun getVehicles(): VehicleResult<List<Vehicle>>
    suspend fun getStatus(vin: String): VehicleResult<VehicleStatus>
    suspend fun lock(vin: String): VehicleResult<Unit>
    suspend fun unlock(vin: String): VehicleResult<Unit>
    suspend fun honkAndFlash(vin: String): VehicleResult<Unit>
    suspend fun sendDestination(vin: String, latitude: Double, longitude: Double, name: String): VehicleResult<Unit>
}
