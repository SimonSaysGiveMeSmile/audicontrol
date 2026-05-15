package com.audicontrol.data

import com.audicontrol.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class VinDecodeResult(
    val vin: String,
    val year: Int? = null,
    val make: String? = null,
    val model: String? = null,
    val trim: String? = null,
    val engine: String? = null,
    val bodyStyle: String? = null,
    val transmission: String? = null,
    val exteriorColor: String? = null,
    val interiorColor: String? = null,
    val msrp: Int? = null,
    val marketPrice: Int? = null,
    val mileage: Int? = null,
    val photos: List<String> = emptyList(),
    val carfaxUrl: String? = null,
    val dealer: String? = null
)

object VinLookupService {

    private val API_KEY = BuildConfig.AUTO_DEV_API_KEY
    private const val BASE_URL = "https://api.auto.dev"

    private val client = OkHttpClient()

    suspend fun decodeVin(vin: String): VinDecodeResult? = withContext(Dispatchers.IO) {
        val cleanVin = vin.trim().uppercase()
        if (cleanVin.length != 17) return@withContext null

        val decoded = fetchVinDecode(cleanVin) ?: return@withContext null
        val photos = fetchPhotos(cleanVin)
        val marketData = fetchMarketData(decoded)

        decoded.copy(
            photos = photos,
            trim = marketData?.trim ?: decoded.trim,
            engine = marketData?.engine ?: decoded.engine,
            bodyStyle = marketData?.bodyStyle ?: decoded.bodyStyle,
            transmission = marketData?.transmission ?: decoded.transmission,
            exteriorColor = marketData?.exteriorColor,
            interiorColor = marketData?.interiorColor,
            msrp = marketData?.msrp,
            marketPrice = marketData?.marketPrice,
            mileage = marketData?.mileage,
            carfaxUrl = marketData?.carfaxUrl,
            dealer = marketData?.dealer
        )
    }

    private fun fetchVinDecode(vin: String): VinDecodeResult? {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/vin/$vin")
                .header("X-API-Key", API_KEY)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val vehicle = json.optJSONObject("vehicle") ?: return null
            return VinDecodeResult(
                vin = vin,
                year = vehicle.optInt("year", 0).takeIf { it > 0 },
                make = vehicle.optString("make").takeIf { it.isNotBlank() },
                model = vehicle.optString("model").takeIf { it.isNotBlank() }
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun fetchPhotos(vin: String): List<String> {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/photos/$vin")
                .header("X-API-Key", API_KEY)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val json = JSONObject(body)
            val retail = json.optJSONObject("data")?.optJSONArray("retail") ?: return emptyList()
            val urls = mutableListOf<String>()
            for (i in 0 until retail.length()) {
                urls.add(retail.getString(i))
            }
            return urls
        } catch (_: Exception) {
            return emptyList()
        }
    }

    private fun fetchMarketData(decoded: VinDecodeResult): VinDecodeResult? {
        val make = decoded.make ?: return null
        val model = decoded.model ?: return null
        val year = decoded.year ?: return null
        try {
            val request = Request.Builder()
                .url("$BASE_URL/listings?vehicle.make=$make&vehicle.model=$model&vehicle.year=$year")
                .header("X-API-Key", API_KEY)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val records = json.optJSONArray("records") ?: return null
            if (records.length() == 0) return null
            val listing = records.getJSONObject(0)
            val vehicle = listing.optJSONObject("vehicle")
            val retail = listing.optJSONObject("retailListing")
            return VinDecodeResult(
                vin = decoded.vin,
                trim = vehicle?.optString("trim")?.takeIf { it.isNotBlank() },
                engine = vehicle?.optString("engine")?.takeIf { it.isNotBlank() },
                bodyStyle = vehicle?.optString("bodyStyle")?.takeIf { it.isNotBlank() },
                transmission = vehicle?.optString("transmission")?.takeIf { it.isNotBlank() },
                exteriorColor = vehicle?.optString("exteriorColor")?.takeIf { it.isNotBlank() },
                interiorColor = vehicle?.optString("interiorColor")?.takeIf { it.isNotBlank() },
                msrp = vehicle?.optInt("baseMsrp", 0)?.takeIf { it > 0 },
                marketPrice = retail?.optInt("price", 0)?.takeIf { it > 0 },
                mileage = retail?.optInt("miles", 0)?.takeIf { it > 0 },
                carfaxUrl = retail?.optString("carfaxUrl")?.takeIf { it.isNotBlank() },
                dealer = retail?.optString("dealer")?.takeIf { it.isNotBlank() }
            )
        } catch (_: Exception) {
            return null
        }
    }
}
