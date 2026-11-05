package com.pwexpress.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class HttpResposta(val codigo: Int, val corpo: String) {
    val ok: Boolean get() = codigo in 200..299
}

/** Cliente HTTP mínimo para a API REST do Supabase (Auth + PostgREST). */
class SupabaseHttp(private val baseUrl: String, private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .build()

    private val tipoJson = "application/json; charset=utf-8".toMediaType()

    /** Lança IOException quando não há conexão. */
    suspend fun post(caminho: String, corpo: String, bearer: String? = null): HttpResposta =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder()
                .url(baseUrl + caminho)
                .post(corpo.toRequestBody(tipoJson))
                .header("apikey", apiKey)
            if (bearer != null) builder.header("Authorization", "Bearer $bearer")
            client.newCall(builder.build()).execute().use { r ->
                HttpResposta(r.code, r.body?.string().orEmpty())
            }
        }
}
