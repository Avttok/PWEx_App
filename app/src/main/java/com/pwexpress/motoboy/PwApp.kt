package com.pwexpress.motoboy

import android.app.Application
import com.pwexpress.core.PwCore
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.core.perfil.Perfis
import com.pwexpress.core.notificacoes.CorridaEmCurso
import com.pwexpress.core.notificacoes.PwNotificacoes
import com.pwexpress.core.notificacoes.ResumoCorrida
import com.pwexpress.core.update.Atualizador
import com.pwexpress.feature.corridas.data.Corrida
import com.pwexpress.feature.corridas.data.StatusParada
import com.pwexpress.feature.corridas.data.TipoParada
import com.pwexpress.feature.gestor.data.Gestao
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
        TemaApp.init(this)
        PwCore.init(this)
        Tracking.init(this, BuildConfig.VERSION_NAME)
        Corridas.init(this)
        Atualizador.init(this, BuildConfig.VERSION_CODE)
        PwNotificacoes.criarCanais(this)
        ligarModulos()
    }

    /** Único ponto onde os módulos se conhecem. */
    private fun ligarModulos() {
        // modo teste → cor das notificações (violeta)
        escopo.launch { TemaApp.modoTeste.collect { PwNotificacoes.modoTeste = it } }
        // corrida ativa (feature:corridas) → notificação ao vivo (serviço do GPS, feature:tracking)
        escopo.launch { Corridas.ativa.collect { CorridaEmCurso.publicar(it?.let(::resumo)) } }
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
            Perfis.perfil.collect { p ->
                // modo teste: o app inteiro fica violeta
                TemaApp.definirModoTeste(p?.modoTeste == true)
                if (p == null) return@collect
                // "Logar com": mantém nome e foto atualizados na conta salva
                PwCore.auth.sessao.value?.email?.let { PwCore.auth.contas.atualizarPerfil(it, p.nome, p.fotoUrl) }
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
                    Gestao.limpar()
                }
            }
        }
    }

    private fun resumo(c: Corrida): ResumoCorrida {
        val atual = c.paradaAtual
        val feitas = c.paradas.count { it.status == StatusParada.CONCLUIDA }
        val titulo = when {
            atual == null -> "Finalizando"
            atual.status == StatusParada.CHEGOU -> if (atual.tipo == TipoParada.COLETA) "No local da coleta" else "No local da entrega"
            atual.tipo == TipoParada.COLETA -> "A caminho da coleta"
            else -> "A caminho da entrega" + if (c.paradas.count { it.tipo == TipoParada.ENTREGA } > 1) " ${c.paradas.filter { it.tipo == TipoParada.ENTREGA }.indexOf(atual) + 1}" else ""
        }
        return ResumoCorrida(
            numero = c.numeroExibicao,
            titulo = titulo,
            endereco = atual?.endereco.orEmpty(),
            etapaAtual = feitas,
            etapas = c.paradas.map { it.tipo == TipoParada.COLETA },
            distanciaM = null,
            valor = c.valorMotoboy?.let { java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("pt-BR")).format(it) },
            teste = c.teste,
        )
    }
}
