package com.audicontrol.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface AudiApiService {

    @GET("usermanagement/users/v1/Audi/US/vehicles")
    suspend fun getVehicles(): Response<ResponseBody>

    @GET("vehicleMgmt/vehicledata/v2/Audi/US/vehicles/{vin}/selectivestatus")
    suspend fun getVehicleStatus(@Path("vin") vin: String): Response<ResponseBody>

    @POST("bs/rlu/v1/Audi/US/vehicles/{vin}/actions")
    suspend fun lockUnlock(
        @Path("vin") vin: String,
        @Body body: okhttp3.RequestBody
    ): Response<ResponseBody>

    @POST("bs/rhf/v1/Audi/US/vehicles/{vin}/actions")
    suspend fun honkAndFlash(
        @Path("vin") vin: String,
        @Body body: okhttp3.RequestBody
    ): Response<ResponseBody>

    @POST("bs/navi/v1/Audi/US/vehicles/{vin}/destinations")
    suspend fun sendDestination(
        @Path("vin") vin: String,
        @Body body: okhttp3.RequestBody
    ): Response<ResponseBody>
}
