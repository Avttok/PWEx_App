package com.pwexpress.core.mapa

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

/**
 * Caminho pelas ruas entre os pontos (na ordem), para todo mapa mostrar o trajeto
 * mesmo quando a rota não veio pronta do servidor. Guarda em memória para não pedir de novo.
 */
object Caminho {
    private val cache = object : LinkedHashMap<String, String>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 80
    }

    suspend fun entre(pontos: List<Pair<Double, Double>>): String? {
        if (pontos.size < 2) return null
        val coords = pontos.take(25).joinToString(";") { "%.6f,%.6f".format(java.util.Locale.US, it.second, it.first) }
        synchronized(cache) { cache[coords] }?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val c = URL("https://router.project-osrm.org/route/v1/driving/$coords?overview=full&geometries=polyline").openConnection() as HttpURLConnection
                c.connectTimeout = 8_000; c.readTimeout = 10_000
                c.setRequestProperty("User-Agent", "PWExpress-app (pwexpress.vercel.app)")
                val corpo = c.inputStream.bufferedReader().use { it.readText() }
                val r = (Json.parseToJsonElement(corpo).jsonObject["routes"] as JsonArray).first().jsonObject
                r["geometry"]!!.jsonPrimitive.content.also { g -> synchronized(cache) { cache[coords] = g } }
            }.getOrNull()
        }
    }
}
