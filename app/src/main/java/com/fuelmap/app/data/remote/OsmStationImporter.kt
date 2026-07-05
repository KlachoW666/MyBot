package com.fuelmap.app.data.remote

import com.fuelmap.app.data.local.GasStationEntity
import com.fuelmap.app.data.local.RegionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Загружает реальные АЗС из OpenStreetMap через Overpass API по границам региона.
 * Бесплатно и без ключа. Объекты с amenity=fuel (узлы и контуры).
 */
object OsmStationImporter {

    private val ENDPOINTS = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter"
    )

    suspend fun fetchStations(region: RegionEntity): List<GasStationEntity> = withContext(Dispatchers.IO) {
        val bbox = "${region.minLat},${region.minLng},${region.maxLat},${region.maxLng}"
        val query = """
            [out:json][timeout:90];
            (
              node["amenity"="fuel"]($bbox);
              way["amenity"="fuel"]($bbox);
            );
            out center tags;
        """.trimIndent()

        val body = "data=" + URLEncoder.encode(query, "UTF-8")
        val response = ENDPOINTS.firstNotNullOfOrNull { url -> runCatching { post(url, body) }.getOrNull() }
            ?: return@withContext emptyList()

        parse(response, region.id)
    }

    private fun post(endpoint: String, body: String): String {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 30_000
            readTimeout = 120_000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("User-Agent", "RussiaOil/1.0 (fuel availability map)")
        }
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        if (conn.responseCode !in 200..299) {
            conn.disconnect()
            error("HTTP ${conn.responseCode}")
        }
        return conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
    }

    private fun parse(json: String, regionId: Long): List<GasStationEntity> {
        val elements = JSONObject(json).optJSONArray("elements") ?: return emptyList()
        val result = ArrayList<GasStationEntity>(elements.length())
        for (i in 0 until elements.length()) {
            val el = elements.optJSONObject(i) ?: continue
            val lat: Double
            val lng: Double
            if (el.has("lat") && el.has("lon")) {
                lat = el.optDouble("lat")
                lng = el.optDouble("lon")
            } else {
                val center = el.optJSONObject("center") ?: continue
                lat = center.optDouble("lat")
                lng = center.optDouble("lon")
            }
            if (lat.isNaN() || lng.isNaN()) continue

            val tags = el.optJSONObject("tags")
            val name = tags?.optString("name").orEmpty()
            val brand = tags?.optString("brand").orEmpty()
            val operator = tags?.optString("operator").orEmpty()
            val street = tags?.optString("addr:street").orEmpty()
            val house = tags?.optString("addr:housenumber").orEmpty()
            val city = tags?.optString("addr:city").orEmpty()

            val displayName = name.ifBlank { brand.ifBlank { operator.ifBlank { "АЗС" } } }
            val displayBrand = brand.ifBlank { operator.ifBlank { name } }
            val address = listOf(city, street, house).filter { it.isNotBlank() }.joinToString(", ")

            result += GasStationEntity(
                name = displayName,
                brand = displayBrand,
                lat = lat,
                lng = lng,
                address = address,
                regionId = regionId
            )
        }
        return result
    }
}
