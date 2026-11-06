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
                // toca primeiro (não depende de rede), depois busca os detalhes
                Alarme.mostrarOferta(applicationContext)
                runBlocking { withTimeoutOrNull(8_000) { Corridas.sincronizarAgora() } }
            }
            "cancelada" -> {
                Alarme.pararOferta(applicationContext)
                Alarme.avisar(applicationContext, "Corrida cancelada", "O gestor cancelou a corrida.")
                runBlocking { withTimeoutOrNull(8_000) { Corridas.sincronizarAgora() } }
            }
        }
    }
}
