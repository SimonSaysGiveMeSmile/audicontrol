package com.audicontrol.data

import com.audicontrol.auth.AuthManager
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class MyAudiBackend(private val authManager: AuthManager) : VehicleBackend {

    override val capabilities = setOf(
        Capability.STATUS,
        Capability.LOCK_CONTROL,
        Capability.HONK_FLASH,
        Capability.SEND_DESTINATION
    )

    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(authManager.tokenManager) { authManager.refreshTokens() })
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: AudiApiService = Retrofit.Builder()
        .baseUrl("https://msg.volkswagen.de/fs-car/")
        .client(client)
        .build()
        .create(AudiApiService::class.java)

    override suspend fun getVehicles(): VehicleResult<List<Vehicle>> {
        return try {
            val response = api.getVehicles()
            if (!response.isSuccessful) {
                return VehicleResult.Error("Failed to fetch vehicles: ${response.code()}")
            }
            val body = response.body()?.string() ?: return VehicleResult.Error("Empty response")
            val vehicles = parseVehicles(body)
            VehicleResult.Success(vehicles)
        } catch (e: Exception) {
            VehicleResult.Error(e.message ?: "Network error")
        }
    }

    override suspend fun getStatus(vin: String): VehicleResult<VehicleStatus> {
        return try {
            val response = api.getVehicleStatus(vin)
            if (!response.isSuccessful) {
                return VehicleResult.Error("Failed to fetch status: ${response.code()}")
            }
            val body = response.body()?.string() ?: return VehicleResult.Error("Empty response")
            val status = parseStatus(vin, body)
            VehicleResult.Success(status)
        } catch (e: Exception) {
            VehicleResult.Error(e.message ?: "Network error")
        }
    }

    override suspend fun lock(vin: String): VehicleResult<Unit> {
        return executeAction {
            val body = buildJsonObject {
                put("lock", JsonPrimitive(true))
            }.toString().toRequestBody("application/json".toMediaType())
            api.lockUnlock(vin, body)
        }
    }

    override suspend fun unlock(vin: String): VehicleResult<Unit> {
        return executeAction {
            val body = buildJsonObject {
                put("lock", JsonPrimitive(false))
            }.toString().toRequestBody("application/json".toMediaType())
            api.lockUnlock(vin, body)
        }
    }

    override suspend fun honkAndFlash(vin: String): VehicleResult<Unit> {
        return executeAction {
            val body = buildJsonObject {
                put("honk", JsonPrimitive(true))
                put("flash", JsonPrimitive(true))
            }.toString().toRequestBody("application/json".toMediaType())
            api.honkAndFlash(vin, body)
        }
    }

    override suspend fun sendDestination(
        vin: String, latitude: Double, longitude: Double, name: String
    ): VehicleResult<Unit> {
        return executeAction {
            val body = buildJsonObject {
                put("destination", buildJsonObject {
                    put("latitude", JsonPrimitive(latitude))
                    put("longitude", JsonPrimitive(longitude))
                    put("name", JsonPrimitive(name))
                })
            }.toString().toRequestBody("application/json".toMediaType())
            api.sendDestination(vin, body)
        }
    }

    private suspend fun executeAction(
        call: suspend () -> retrofit2.Response<okhttp3.ResponseBody>
    ): VehicleResult<Unit> {
        return try {
            val response = call()
            if (response.isSuccessful) VehicleResult.Success(Unit)
            else VehicleResult.Error("Action failed: ${response.code()}")
        } catch (e: Exception) {
            VehicleResult.Error(e.message ?: "Network error")
        }
    }

    private fun parseVehicles(body: String): List<Vehicle> {
        val root = json.parseToJsonElement(body).jsonObject
        val vehiclesArray = root["userVehicles"]?.jsonObject
            ?.get("vehicle")?.jsonArray ?: return emptyList()

        return vehiclesArray.mapNotNull { element ->
            val obj = element.jsonObject
            val vin = obj["vin"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val model = obj["model"]?.jsonPrimitive?.content ?: "Audi"
            val year = obj["modelYear"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val nickname = obj["nickname"]?.jsonPrimitive?.content
            Vehicle(vin = vin, model = model, year = year, nickname = nickname)
        }
    }

    private fun parseStatus(vin: String, body: String): VehicleStatus {
        val root = json.parseToJsonElement(body).jsonObject

        fun findField(name: String): String? {
            val fields = root["vehicleData"]?.jsonObject
                ?.get("data")?.jsonArray ?: return null
            for (section in fields) {
                val sectionFields = section.jsonObject["field"]?.jsonArray ?: continue
                for (field in sectionFields) {
                    if (field.jsonObject["id"]?.jsonPrimitive?.content == name) {
                        return field.jsonObject["value"]?.jsonPrimitive?.content
                    }
                }
            }
            return null
        }

        return VehicleStatus(
            vin = vin,
            locked = findField("0x0301040001")?.contains("2") == true,
            fuelLevelPercent = findField("0x030103000A")?.toIntOrNull() ?: 0,
            rangeKm = findField("0x0301030006")?.toIntOrNull() ?: 0,
            odometerKm = findField("0x0101010002")?.toIntOrNull() ?: 0,
            doorsOpen = emptyList(),
            windowsOpen = emptyList(),
            latitude = findField("0x0301030001")?.toDoubleOrNull(),
            longitude = findField("0x0301030002")?.toDoubleOrNull(),
            lastUpdated = System.currentTimeMillis()
        )
    }
}