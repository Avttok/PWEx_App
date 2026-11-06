package com.pwexpress.feature.corridas.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pwexpress.feature.corridas.data.Corridas
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/** Recebe mensagens do servidor (via Firebase), mesmo com o app fechado. */
class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Corridas.registrarToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        when (message.data["tipo"]) {
            "oferta" -> {
                // toca primeiro (não depende de rede), depois busca os detalhes e enriquece a notificação
                val id = message.data["oferta_id"]
                Alarme.mostrarOferta(applicationContext, ofertaId = id)
                runBlocking { withTimeoutOrNull(8_000) { Corridas.sincronizarAgora() } }
                Corridas.oferta.value?.takeIf { id == null || it.id == id }?.let { Alarme.mostrarOferta(applicationContext, it) }
            }
            "planejada_nova", "planejada_voltou", "planejada_reservada", "planejada_liberada",
            "planejada_reagendada", "planejada_lembrete", "planejada_atrasada" -> {
                Alarme.planejada(applicationContext, message.data["tipo"]!!, message.data["corrida_id"], message.data["agendada_para"])
                runBlocking { withTimeoutOrNull(6_000) { Corridas.carregarPlanejadas() } }
            }
            "corrida_voltou" -> Alarme.avisarSilencioso(applicationContext, "Uma corrida que você recusou voltou ao lançamento")
            "cancelada" -> {
                Alarme.pararOferta(applicationContext)
                Alarme.avisar(applicationContext, "Corrida cancelada", "O gestor cancelou a corrida.",
                    "A corrida foi cancelada pelo gestor. Se você estava a caminho, pode parar. Nenhuma etapa a mais é necessária.")
                runBlocking { withTimeoutOrNull(8_000) { Corridas.sincronizarAgora() } }
            }
        }
    }
}
