package com.pwexpress.motoboy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pwexpress.core.PwCore
import com.pwexpress.core.designsystem.PwAba
import com.pwexpress.core.designsystem.PwBarraAbas
import com.pwexpress.core.designsystem.PwFundo
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwTheme
import com.pwexpress.core.designsystem.PwTopo
import com.pwexpress.core.perfil.Perfis
import com.pwexpress.core.update.Atualizador
import com.pwexpress.feature.auth.LoginScreen
import com.pwexpress.feature.corridas.data.Corridas
import com.pwexpress.feature.corridas.ui.CartaoOfertas
import com.pwexpress.feature.corridas.ui.CorridaScreen
import com.pwexpress.feature.corridas.ui.OfertaScreen
import com.pwexpress.feature.corridas.ui.PlanejadasScreen
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.ui.GestorApp
import com.pwexpress.feature.gestor.ui.GestorNav
import com.pwexpress.feature.perfil.CartaoGanhos
import com.pwexpress.feature.perfil.Linha
import com.pwexpress.feature.perfil.NomePerfil
import com.pwexpress.feature.perfil.PerfilScreen
import com.pwexpress.feature.tracking.Modo
import com.pwexpress.feature.tracking.Tracking
import com.pwexpress.feature.tracking.ui.TrackingScreen
import com.pwexpress.motoboy.ui.AcoesTopo
import com.pwexpress.motoboy.ui.JanelaAtualizacao
import com.pwexpress.motoboy.ui.JanelaConta
import com.pwexpress.motoboy.ui.Painel
import com.pwexpress.motoboy.ui.PrimeiraConfiguracao
import com.pwexpress.motoboy.ui.lerPreparo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** Pedido vindo de uma notificação (ex.: abrir a aba Planejadas). */
    private val abrirAba = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        abrirAba.value = intent?.getStringExtra("pwex_abrir")
        val prefs = getSharedPreferences("pwex_painel", Context.MODE_PRIVATE)
        setContent {
            PwTheme {
                PwFundo {
                    val sessao by PwCore.auth.sessao.collectAsState()
                    val oferta by Corridas.oferta.collectAsState()
                    val ativa by Corridas.ativa.collectAsState()
                    val planejadas by Corridas.planejadas.collectAsState()
                    val info by Tracking.info.collectAsState()
                    val perfil by Perfis.perfil.collectAsState()
                    var noPerfil by remember { mutableStateOf(false) }
                    var naConfig by remember { mutableStateOf(false) }
                    var conta by remember { mutableStateOf(false) }
                    var aba by rememberSaveable { mutableStateOf("inicio") }
                    var painel by remember { mutableStateOf(runCatching { Painel.valueOf(prefs.getString("painel", "MOTOBOY")!!) }.getOrDefault(Painel.MOTOBOY)) }
                    val scope = rememberCoroutineScope()
                    val ciclo = LocalLifecycleOwner.current
                    val ctx = LocalContext.current
                    var preparado by remember { mutableStateOf(lerPreparo(ctx).completo) }
                    // depois de entrar, espera o perfil (papéis) antes de decidir o lado do app
                    var esperouPerfil by remember { mutableStateOf(false) }
                    LaunchedEffect(sessao?.email) { esperouPerfil = false; delay(8_000); esperouPerfil = true }

                    LaunchedEffect(abrirAba.value) { abrirAba.value?.let { if (it == "planejadas") { aba = "corridas"; painel = Painel.MOTOBOY; naConfig = false }; abrirAba.value = null } }

                    // se algo for desligado depois, a tela de preparo volta
                    LaunchedEffect(Unit) { ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) { preparado = lerPreparo(ctx).completo } }

                    // reserva caso o push falhe: com o app aberto e disponível, consulta a cada 10 s
                    LaunchedEffect(sessao != null, info.modo) {
                        if (sessao == null || info.modo != Modo.DISPONIVEL) return@LaunchedEffect
                        ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                            while (true) { Corridas.sincronizarAgora(); delay(10_000) }
                        }
                    }
                    // planejadas: contador da aba sempre em dia
                    LaunchedEffect(sessao != null, perfil?.motoboy) {
                        if (sessao == null || perfil?.motoboy != true) return@LaunchedEffect
                        ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) { while (true) { Corridas.carregarPlanejadas(); delay(60_000) } }
                    }

                    BackHandler(enabled = noPerfil) { noPerfil = false }
                    BackHandler(enabled = naConfig && !noPerfil) { naConfig = false }
                    BackHandler(enabled = !noPerfil && aba != "inicio" && painel == Painel.MOTOBOY) { aba = "inicio" }

                    val s = sessao
                    val p = perfil
                    val podeTrocar = p?.gestor == true && p.motoboy
                    // gestor sem papel de motoboy vai direto para o painel
                    val efetivo = when {
                        p?.gestor == true && !p.motoboy -> Painel.GESTOR
                        p?.gestor == true -> painel
                        else -> Painel.MOTOBOY
                    }
                    fun trocar(novo: Painel) {
                        painel = novo
                        prefs.edit { putString("painel", novo.name) }
                        conta = false
                    }
                    fun sair() {
                        conta = false; noPerfil = false
                        Gestao.limpar()
                        scope.launch { Tracking.sairDaConta() }
                    }
                    val conteudo = Modifier.safeDrawingPadding()
                    val o = oferta
                    val a = ativa

                    when {
                        s == null -> LoginScreen(conteudo)
                        p == null && !esperouPerfil -> Box(conteudo.fillMaxSize(), contentAlignment = Alignment.Center) {
                            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                com.pwexpress.core.designsystem.PwLogoGrande(170.dp)
                                com.pwexpress.core.designsystem.PwCarregando(texto = "Carregando sua conta…")
                            }
                        }
                        noPerfil -> PerfilScreen(
                            email = s.email,
                            onVoltar = { noPerfil = false },
                            onSair = ::sair,
                            modifier = conteudo,
                            diagnostico = {
                                Linha("Estado do GPS", info.modo.rotulo)
                                Linha("Pontos na fila", info.naFila.toString())
                                Linha("Último erro", info.erro ?: "nenhum")
                            },
                        )
                        efetivo == Painel.GESTOR -> GestorApp(
                            modifier = conteudo,
                            topo = { PwTopo { AcoesTopo(efetivo, podeTrocar, ::trocar) { conta = true } } },
                            meuId = p?.id,
                        )
                        o != null -> OfertaScreen(o, onFim = { }, modifier = conteudo)
                        !preparado -> PrimeiraConfiguracao(onPronto = { preparado = true }, modifier = conteudo)
                        a != null -> CorridaScreen(a, admin = p?.admin == true, modifier = conteudo)
                        naConfig && efetivo == Painel.MOTOBOY -> com.pwexpress.motoboy.ui.ConfigMotoApp(onVoltar = { naConfig = false }, modifier = conteudo)
                        else -> Box(conteudo.fillMaxSize()) {
                            val comPlanejadas = p?.modulo("corridas_planejadas") != false
                            val comCarteira = p?.modulo("carteira") != false
                            val abaMotoboy = when { aba == "corridas" -> "corridas"; aba == "carteira" && comCarteira -> "carteira"; else -> "inicio" }
                            androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
                                PwTopo { AcoesTopo(efetivo, podeTrocar, ::trocar) { conta = true } }
                                Box(Modifier.weight(1f).padding(bottom = 86.dp)) {
                                    when (abaMotoboy) {
                                        "corridas" -> PlanejadasScreen(onIniciada = { aba = "inicio" }, comPlanejadas = comPlanejadas)
                                        "carteira" -> com.pwexpress.feature.corridas.ui.CarteiraScreen()
                                        else -> TrackingScreen(
                                            saudacao = NomePerfil(s.email).substringBefore(" "),
                                            extras = { CartaoGanhos(onCarteira = if (comCarteira) ({ aba = "carteira" }) else null) },
                                            chips = { com.pwexpress.feature.tracking.ui.ChipInicio(PwIcones.Radar, com.pwexpress.motoboy.ui.textoRaio(p?.raioKm, p?.raioModo ?: "posicao")) { naConfig = true } },
                                            onConfiguracoes = { naConfig = true },
                                        )
                                    }
                                }
                            }
                            val reservas = planejadas.count { it.minha }
                            val livres = planejadas.count { !it.minha && it.status == "agendada" }
                            PwBarraAbas(
                                listOfNotNull(
                                    PwAba("inicio", "Início", PwIcones.House),
                                    PwAba("corridas", "Corridas", PwIcones.Route, if (comPlanejadas) livres + reservas else 0),
                                    if (comCarteira) PwAba("carteira", "Carteira", PwIcones.Wallet) else null,
                                ),
                                abaMotoboy, { aba = it },
                                Modifier.align(Alignment.BottomCenter),
                            )
                        }
                    }

                    if (conta && s != null) {
                        JanelaConta(
                            s.email, efetivo, podeTrocar,
                            onFechar = { conta = false },
                            onPerfil = { conta = false; noPerfil = true },
                            onConfiguracoes = { conta = false; if (efetivo == Painel.GESTOR) GestorNav.abrirConfiguracoes() else naConfig = true },
                            onTrocar = ::trocar, onSair = ::sair,
                            onDev = { conta = false; if (efetivo != Painel.GESTOR) trocar(Painel.GESTOR); GestorNav.abrirDev() },
                        )
                    }
                    JanelaAtualizacao()
                    com.pwexpress.core.designsystem.PwToasts(Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("pwex_abrir")?.let { abrirAba.value = it }
    }

    override fun onStart() {
        super.onStart()
        if (PwCore.auth.sessao.value != null) {
            Tracking.retomarSeAtivo()
            Corridas.sincronizar()
        }
        // procura versão nova uma vez por abertura do app (só abre a janela se for nova)
        if (!verificouAtualizacao) {
            verificouAtualizacao = true
            if (Perfis.perfil.value?.modulo("atualizacao_app") != false) Atualizador.verificar(automatico = true)
        }
    }

    private companion object { var verificouAtualizacao = false }
}
