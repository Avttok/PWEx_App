package com.pwexpress.feature.gestor.data

import com.pwexpress.core.PwCore
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.int
import com.pwexpress.core.json.obj
import com.pwexpress.core.json.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.FormBody
import okhttp3.Request
import java.net.URLEncoder

data class EnderecoG(val descricao: String, val lat: Double, val lng: Double)
data class RotaG(val distanciaM: Int, val duracaoS: Int, val polyline: String)

/**
 * Endereços, rotas e pontos próximos (OpenStreetMap: Nominatim, OSRM e Overpass), os mesmos do site.
 * Para trocar pelo Google, basta reescrever este arquivo.
 */
object Geo {
    private const val UA = "PWExpress-app (pwexpress.vercel.app)"

    private suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            PwCore.http.client.newCall(Request.Builder().url(url).header("User-Agent", UA).header("Accept-Language", "pt-BR").build())
                .execute().use { if (it.isSuccessful) it.body?.string() else null }
        }.getOrNull()
    }

    private fun encurtar(nome: String) = nome.split(", ").filter { it != "Brasil" && it != "Região Sul" }.take(5).joinToString(", ")

    suspend fun buscarEnderecos(texto: String): List<EnderecoG> {
        if (texto.trim().length < 4) return emptyList()
        val corpo = get("https://nominatim.openstreetmap.org/search?format=jsonv2&countrycodes=br&limit=6&accept-language=pt-BR&q=${URLEncoder.encode(texto, "UTF-8")}") ?: return emptyList()
        return runCatching {
            (Json.parseToJsonElement(corpo) as JsonArray).mapNotNull { e ->
                val o = e as? JsonObject ?: return@mapNotNull null
                EnderecoG(encurtar(o.str("display_name").orEmpty()), o.dbl("lat") ?: return@mapNotNull null, o.dbl("lon") ?: return@mapNotNull null)
            }
        }.getOrDefault(emptyList())
    }

    suspend fun enderecoDoPonto(lat: Double, lng: Double): String {
        val corpo = get("https://nominatim.openstreetmap.org/reverse?format=jsonv2&accept-language=pt-BR&lat=$lat&lon=$lng")
        return corpo?.let { runCatching { encurtar(Json.parseToJsonElement(it).jsonObject.str("display_name").orEmpty()) }.getOrNull() }
            ?.ifBlank { null } ?: "%.5f, %.5f".format(lat, lng)
    }

    suspend fun calcularRota(pontos: List<Pair<Double, Double>>): RotaG? {
        if (pontos.size < 2) return null
        val coords = pontos.joinToString(";") { "${it.second},${it.first}" }
        val corpo = get("https://router.project-osrm.org/route/v1/driving/$coords?overview=full&geometries=polyline") ?: return null
        return runCatching {
            val o = Json.parseToJsonElement(corpo).jsonObject
            val r = (o["routes"] as JsonArray).first().jsonObject
            RotaG(r.dbl("distance")!!.toInt(), r.dbl("duration")!!.toInt(), r.str("geometry")!!)
        }.getOrNull()
    }

    private val tipos = mapOf(
        "fuel" to "Posto", "pharmacy" to "Farmácia", "supermarket" to "Mercado", "bank" to "Banco", "restaurant" to "Restaurante",
        "school" to "Escola", "hospital" to "Hospital", "place_of_worship" to "Igreja", "bakery" to "Padaria", "cafe" to "Café",
        "fast_food" to "Lanchonete", "convenience" to "Conveniência", "park" to "Praça/parque", "police" to "Polícia", "post_office" to "Correios",
    )

    suspend fun pontosProximos(lat: Double, lng: Double, raioM: Int = 250): List<RefG> = withContext(Dispatchers.IO) {
        val q = "[out:json][timeout:10];(nwr(around:$raioM,$lat,$lng)[name][~\"^(amenity|shop|leisure|tourism)$\"~\".\"];);out center 40;"
        runCatching {
            val req = Request.Builder().url("https://overpass-api.de/api/interpreter").header("User-Agent", UA).post(FormBody.Builder().add("data", q).build()).build()
            val corpo = PwCore.http.client.newCall(req).execute().use { it.body?.string() } ?: return@runCatching emptyList()
            val vistos = mutableSetOf<String>()
            (Json.parseToJsonElement(corpo).jsonObject["elements"] as JsonArray).mapNotNull { e ->
                val o = e as? JsonObject ?: return@mapNotNull null
                val tags = o.obj("tags") ?: return@mapNotNull null
                val nome = tags.str("name") ?: return@mapNotNull null
                val c = o.obj("center")
                val plat = o.dbl("lat") ?: c?.dbl("lat") ?: return@mapNotNull null
                val plng = o.dbl("lon") ?: c?.dbl("lon") ?: return@mapNotNull null
                if (!vistos.add(nome.lowercase())) return@mapNotNull null
                val t = tags.str("amenity") ?: tags.str("shop") ?: tags.str("leisure") ?: tags.str("tourism")
                RefG(nome, metros(lat, lng, plat, plng), tipos[t], plat, plng)
            }.sortedBy { it.distanciaM ?: 0 }.take(8)
        }.getOrDefault(emptyList())
    }

    fun metros(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Int {
        val r = 6371000.0
        val dLat = Math.toRadians(bLat - aLat); val dLng = Math.toRadians(bLng - aLng)
        val h = Math.sin(dLat / 2).let { it * it } + Math.cos(Math.toRadians(aLat)) * Math.cos(Math.toRadians(bLat)) * Math.sin(dLng / 2).let { it * it }
        return (2 * r * Math.asin(Math.sqrt(h))).toInt()
    }

}
