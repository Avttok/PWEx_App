package com.pwexpress.feature.tracking

/** Estados do motoboy. O valor `api` é o mesmo do enum status_motoboy no banco. */
enum class Modo(val api: String, val rotulo: String) {
    OFFLINE("offline", "Offline"),
    DISPONIVEL("disponivel", "Disponível"),
    EM_CORRIDA("em_corrida", "Em corrida");

    companion object {
        fun de(api: String?): Modo = entries.firstOrNull { it.api == api } ?: OFFLINE
    }
}

/** Ritmo do rastreamento. Ajuste aqui sem mexer no resto do código. */
object Intervalos {
    const val CORRIDA_NORMAL_MS = 45_000L
    const val CORRIDA_ECONOMIA_MS = 90_000L

    /** Disponível: lê a cada 60 s, mas só envia a cada 3 min OU se andou 300 m. */
    const val DISPONIVEL_LEITURA_MS = 60_000L
    const val DISPONIVEL_ENVIO_MS = 180_000L
    const val DISPONIVEL_DISTANCIA_M = 300f

    /** Em corrida, descarta leituras com erro maior que isso (evita "saltos" no mapa). */
    const val PRECISAO_MAXIMA_M = 150f

    const val LOTE_MAXIMO = 200

    fun corridaMs(economia: Boolean) = if (economia) CORRIDA_ECONOMIA_MS else CORRIDA_NORMAL_MS
}

/** Estado exibido na tela. */
data class TrackingInfo(
    val modo: Modo = Modo.OFFLINE,
    val economia: Boolean = false,
    val ultimaLeitura: Long? = null,
    val ultimaPrecisao: Float? = null,
    val ultimoEnvio: Long? = null,
    val naFila: Int = 0,
    val enviando: Boolean = false,
    val erro: String? = null,
)
