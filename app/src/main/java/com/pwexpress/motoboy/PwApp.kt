package com.pwexpress.motoboy

import android.app.Application
import com.pwexpress.core.PwCore
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.core.perfil.Perfis
import com.pwexpress.feature.corridas.data.Corridas
import com.pwexpress.feature.tracking.Tracking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class PwApp : Application() {

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        TemaApp.init(this)
        PwCore.init(this)
        Tracking.init(this, BuildConfig.VERSION_NAME)
        Corridas.init(this)
        ligarModulos()
    }

    /** Único ponto onde os módulos se conhecem. */
    private fun ligarModulos() {
        // corrida ativa (feature:corridas) → modo do GPS (feature:tracking)
        escopo.launch {
            combine(Corridas.ativa, Corridas.carregou) { ativa, carregou -> carregou to ativa?.id }
                .distinctUntilChanged()
                .collect { (carregou, corridaId) ->
                    if (!carregou) return@collect
                    if (corridaId != null) Tracking.entrarEmCorrida(corridaId) else {
                        Tracking.sairDaCorrida()
                        Perfis.carregar() // corrida concluída: atualiza ganhos
                    }
                }
        }
        // configurações do servidor (core:perfil) → ritmo do GPS (feature:tracking)
        escopo.launch {
            Perfis.perfil.filterNotNull().collect { p ->
                Tracking.aplicarConfiguracoes(
                    corridaS = p.numero("intervalo_corrida_s", 45.0),
                    economiaS = p.numero("intervalo_economia_s", 90.0),
                    disponivelMin = p.numero("disponivel_envio_min", 3.0),
                    disponivelM = p.numero("disponivel_distancia_m", 300.0),
                )
            }
        }
        // login / logout
        escopo.launch {
            PwCore.auth.sessao.collect { sessao ->
                if (sessao != null) {
                    Corridas.aoEntrar()
                    launch { Perfis.carregar() }
                } else {
                    Corridas.limpar()
                    Perfis.limpar()
                }
            }
        }
    }
}
