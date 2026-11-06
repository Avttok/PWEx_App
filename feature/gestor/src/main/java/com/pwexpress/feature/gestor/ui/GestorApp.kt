package com.pwexpress.feature.gestor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pwexpress.core.designsystem.PwAba
import com.pwexpress.core.designsystem.PwBarraAbas
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.Status
import kotlinx.coroutines.delay

/** Telas empilhadas por cima das abas (detalhes, formulários). */
sealed interface TelaG {
    data class Corrida(val id: String) : TelaG
    data object NovaCorrida : TelaG
    data class Empresa(val id: String?) : TelaG
    data class Membro(val id: String) : TelaG
    data object Financeiro : TelaG
    data object Configuracoes : TelaG
    data object Dev : TelaG
}

/** Pedidos de navegação vindos de fora do painel (ex.: "Configurações" no menu da foto). */
object GestorNav {
    internal val pedido = kotlinx.coroutines.flow.MutableStateFlow<TelaG?>(null)
    fun abrirConfiguracoes() { pedido.value = TelaG.Configuracoes }
    fun abrirDev() { pedido.value = TelaG.Dev }
}

/**
 * Painel do gestor no app: tudo o que o site faz.
 * `topo` é o cabeçalho do app (logo + foto, que abre o menu da conta).
 */
@Composable
fun GestorApp(
    modifier: Modifier = Modifier,
    topo: @Composable () -> Unit,
    meuId: String?,
) {
    var aba by rememberSaveable { mutableStateOf("mapa") }
    val pilha = remember { mutableStateListOf<TelaG>() }
    val dados by Gestao.dados.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    val ciclo = LocalLifecycleOwner.current

    // atualiza enquanto o painel está aberto (o site usa tempo real; aqui, a cada 6 s)
    LaunchedEffect(Unit) {
        ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { Gestao.atualizar(); delay(6_000) }
        }
    }

    fun abrir(t: TelaG) { if (pilha.lastOrNull() != t) pilha.add(t) }
    val pedido by GestorNav.pedido.collectAsState()
    LaunchedEffect(pedido) { pedido?.let { abrir(it); GestorNav.pedido.value = null } }
    fun voltar() { if (pilha.isNotEmpty()) pilha.removeAt(pilha.lastIndex) }
    BackHandler(enabled = pilha.isNotEmpty()) { voltar() }

    // corrida em andamento sem posição do motoboy há 30 min: avisa uma vez por corrida
    val avisadas = remember { mutableSetOf<String>() }
    LaunchedEffect(dados) {
        val limite = System.currentTimeMillis() - 30 * 60_000
        dados.corridas.filter { it.status == Status.ACEITA || it.status == Status.EM_ANDAMENTO }.forEach { cor ->
            val id = cor.motoboyId ?: return@forEach
            val t = dados.posicao(id)?.posicaoEm ?: 0L
            if (t < limite && avisadas.add(cor.id)) {
                val min = if (t > 0) ((System.currentTimeMillis() - t) / 60_000) else null
                com.pwexpress.core.designsystem.PwToast.mostrar(
                    "${dados.membro(id)?.nome ?: "O motoboy"} está na corrida ${cor.numeroExibicao} e mandou a localização pela última vez ${min?.let { "há $it min" } ?: "há muito tempo"}.",
                    com.pwexpress.core.designsystem.TipoAviso.AVISO, "Motoboy sem sinal",
                )
            } else if (t >= limite) avisadas.remove(cor.id)
        }
    }
    val comCarteira = modulo("carteira")
    val planejadas = dados.corridas.count { it.teste == teste && it.status in Status.PLANEJADAS }
    val semMotoboy = dados.corridas.count { it.teste == teste && it.status == Status.SEM_MOTOBOY }

    Box(modifier.fillMaxSize()) {
        val atual = pilha.lastOrNull()
        Column(Modifier.fillMaxSize()) {
            if (atual == null) topo()
            Box(Modifier.weight(1f)) {
                when (atual) {
                    is TelaG.Corrida -> DetalheCorridaG(atual.id, onVoltar = ::voltar)
                    TelaG.NovaCorrida -> NovaCorridaG(onVoltar = ::voltar, onCriada = { id -> voltar(); abrir(TelaG.Corrida(id)) })
                    is TelaG.Empresa -> EmpresaG(atual.id, onVoltar = ::voltar)
                    is TelaG.Membro -> MembroG(atual.id, meuId = meuId, onVoltar = ::voltar)
                    TelaG.Financeiro -> FinanceiroG(onVoltar = ::voltar)
                    TelaG.Configuracoes -> ConfiguracoesG(onVoltar = ::voltar)
                    TelaG.Dev -> DevG(onVoltar = ::voltar)
                    null -> when (aba) {
                        "mapa" -> MapaG(onCorrida = { abrir(TelaG.Corrida(it)) })
                        "corridas" -> CorridasG(onAbrir = { abrir(TelaG.Corrida(it)) }, onNova = { abrir(TelaG.NovaCorrida) })
                        "empresas" -> EmpresasG(onAbrir = { abrir(TelaG.Empresa(it)) })
                        "equipe" -> EquipeG(onAbrir = { abrir(TelaG.Membro(it)) })
                        else -> FinanceiroG(onVoltar = null)
                    }
                }
            }
        }
        if (atual == null) {
            PwBarraAbas(
                listOf(
                    PwAba("mapa", "Mapa", PwIcones.Mapa),
                    PwAba("corridas", "Corridas", PwIcones.Route, planejadas + semMotoboy),
                    PwAba("empresas", "Empresas", PwIcones.Building2),
                    PwAba("equipe", "Equipe", PwIcones.Users),
                ) + (if (comCarteira) listOf(PwAba("financeiro", "Carteira", PwIcones.Wallet)) else emptyList()),
                aba, { aba = it },
                Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
