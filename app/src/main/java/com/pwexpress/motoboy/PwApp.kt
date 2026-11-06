package com.pwexpress.motoboy

import android.app.Application
import com.pwexpress.core.PwCore
import com.pwexpress.feature.corridas.data.Corridas
import com.pwexpress.feature.tracking.Tracking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class PwApp : Application() {

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        PwCore.init(this)
        Tracking.init(this, BuildConfig.VERSION_NAME)
        Corridas.init(this)
        ligarModulos()
    }

    /**
     * Único ponto onde os módulos se conhecem:
     * corrida ativa (feature:corridas) → modo do GPS (feature:tracking).
     */
    private fun ligarModulos() {
        escopo.launch {
            combine(Corridas.ativa, Corridas.carregou) { ativa, carregou -> carregou to ativa?.id }
                .distinctUntilChanged()
                .collect { (carregou, corridaId) ->
                    if (!carregou) return@collect // ainda não sabemos o estado no servidor
                    if (corridaId != null) Tracking.entrarEmCorrida(corridaId) else Tracking.sairDaCorrida()
                }
        }
        escopo.launch {
            PwCore.auth.sessao.collect { sessao ->
                if (sessao != null) Corridas.aoEntrar() else Corridas.limpar()
            }
        }
    }
}
