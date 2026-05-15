package com.audicontrol.data

class CanBridgeBackend : VehicleBackend {

    override val capabilities = setOf(
        Capability.STATUS,
        Capability.LOCK_CONTROL,
        Capability.CLIMATE_CONTROL,
        Capability.INFOTAINMENT_CONTROL
    )

    override suspend fun getVehicles(): VehicleResult<List<Vehicle>> =
        VehicleResult.Error("CAN Bridge not connected")

    override suspend fun getStatus(vin: String): VehicleResult<VehicleStatus> =
        VehicleResult.Error("CAN Bridge not connected")

    override suspend fun lock(vin: String): VehicleResult<Unit> =
        VehicleResult.Error("CAN Bridge not connected")

    override suspend fun unlock(vin: String): VehicleResult<Unit> =
        VehicleResult.Error("CAN Bridge not connected")

    override suspend fun honkAndFlash(vin: String): VehicleResult<Unit> =
        VehicleResult.Error("CAN Bridge not connected")

    override suspend fun sendDestination(
        vin: String, latitude: Double, longitude: Double, name: String
    ): VehicleResult<Unit> =
        VehicleResult.Error("CAN Bridge not connected")
}
