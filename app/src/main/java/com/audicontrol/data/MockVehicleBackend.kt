package com.audicontrol.data

import kotlinx.coroutines.delay

class MockVehicleBackend : VehicleBackend {

    override val capabilities = setOf(
        Capability.STATUS,
        Capability.LOCK_CONTROL,
        Capability.HONK_FLASH,
        Capability.SEND_DESTINATION
    )

    private var locked = true

    private val q8 = Vehicle(
        vin = "WA1AVAF16KD017273",
        model = "Q8 55 TFSI",
        year = 2019,
        nickname = "My Q8"
    )

    override suspend fun getVehicles(): VehicleResult<List<Vehicle>> {
        delay(600)
        return VehicleResult.Success(listOf(q8))
    }

    override suspend fun getStatus(vin: String): VehicleResult<VehicleStatus> {
        delay(800)
        return VehicleResult.Success(
            VehicleStatus(
                vin = vin,
                locked = locked,
                fuelLevelPercent = 74,
                rangeKm = 412,
                odometerKm = 28_340,
                doorsOpen = emptyList(),
                windowsOpen = emptyList(),
                latitude = 40.7128,
                longitude = -74.0060,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    override suspend fun lock(vin: String): VehicleResult<Unit> {
        delay(1200)
        locked = true
        return VehicleResult.Success(Unit)
    }

    override suspend fun unlock(vin: String): VehicleResult<Unit> {
        delay(1200)
        locked = false
        return VehicleResult.Success(Unit)
    }

    override suspend fun honkAndFlash(vin: String): VehicleResult<Unit> {
        delay(1500)
        return VehicleResult.Success(Unit)
    }

    override suspend fun sendDestination(
        vin: String, latitude: Double, longitude: Double, name: String
    ): VehicleResult<Unit> {
        delay(1000)
        return VehicleResult.Success(Unit)
    }
}
