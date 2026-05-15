package com.audicontrol.data

import com.audicontrol.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object VehicleImageService {

    private val API_KEY = BuildConfig.AUTO_DEV_API_KEY
    private const val BASE_URL = "https://api.auto.dev"

    private val client = OkHttpClient()

    private val fallbackImages = mapOf(
        "Q8" to "https://upload.wikimedia.org/wikipedia/commons/f/f7/2019_Audi_Q8_Front.jpg",
        "Q7" to "https://upload.wikimedia.org/wikipedia/commons/3/3d/2020_Audi_Q7_front_5.19.19.jpg",
        "A4" to "https://upload.wikimedia.org/wikipedia/commons/4/44/2017_Audi_A4_Premium_quattro_front_3.16.18.jpg",
        "A6" to "https://upload.wikimedia.org/wikipedia/commons/3/3e/2019_Audi_A6_front_4.18.19.jpg",
        "e-tron" to "https://upload.wikimedia.org/wikipedia/commons/5/5d/2019_Audi_e-tron_55_quattro_front_4.18.19.jpg",
        "Q5" to "https://upload.wikimedia.org/wikipedia/commons/7/7e/2018_Audi_Q5_front_4.2.18.jpg"
    )

    fun getImageUrl(model: String): String {
        val entry = fallbackImages.entries.firstOrNull { model.contains(it.key, ignoreCase = true) }
        return entry?.value ?: fallbackImages["Q8"]!!
    }

    suspend fun getPhotoFromApi(vin: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/photos/$vin")
                .header("X-API-Key", API_KEY)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val retail = json.getJSONObject("data").getJSONArray("retail")
            if (retail.length() > 0) retail.getString(0) else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getPhotosByModel(make: String, model: String, year: Int): List<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/listings?vehicle.make=$make&vehicle.model=$model&vehicle.year=$year")
                .header("X-API-Key", API_KEY)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val records = json.getJSONArray("records")
            val urls = mutableListOf<String>()
            for (i in 0 until minOf(records.length(), 5)) {
                val listing = records.getJSONObject(i)
                val retail = listing.optJSONObject("retailListing")
                val img = retail?.optString("primaryImage")
                if (!img.isNullOrBlank()) urls.add(img)
            }
            urls
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun createAuthenticatedClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                if (original.url.host.contains("auto.dev") || original.url.host.contains("photos.vin")) {
                    chain.proceed(
                        original.newBuilder()
                            .header("X-API-Key", API_KEY)
                            .build()
                    )
                } else {
                    chain.proceed(original)
                }
            }
            .build()
    }
}
