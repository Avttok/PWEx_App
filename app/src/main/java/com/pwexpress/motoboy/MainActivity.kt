package com.pwexpress.motoboy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pwexpress.core.PwCore
import com.pwexpress.core.designsystem.PwFundo
import com.pwexpress.core.designsystem.PwTheme
import com.pwexpress.core.perfil.Perfis
import com.pwexpress.feature.auth.LoginScreen
import com.pwexpress.feature.corridas.data.Corridas
import com.pwexpress.feature.corridas.ui.CartaoOfertas
import com.pwexpress.feature.corridas.ui.CorridaScreen
import com.pwexpress.feature.corridas.ui.OfertaScreen
import com.pwexpress.feature.perfil.AvatarPerfil
import com.pwexpress.feature.perfil.CartaoGanhos
import com.pwexpress.feature.perfil.Linha
import com.pwexpress.feature.perfil.NomePerfil
import com.pwexpress.feature.perfil.PerfilScreen
import com.pwexpress.feature.tracking.Modo
import com.pwexpress.feature.tracking.Tracking
import com.pwexpress.feature.tracking.ui.TrackingScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PwTheme {
                PwFundo {
                    val sessao by PwCore.auth.sessao.collectAsState()
                    val oferta by Corridas.oferta.collectAsState()
                    val ativa by Corridas.ativa.collectAsState()
                    val info by Tracking.info.collectAsState()
                    val perfil by Perfis.perfil.collectAsState()
                    var noPerfil by remember { mutableStateOf(false) }
                    val scope = rememberCoroutineScope()
                    val ciclo = LocalLifecycleOwner.current

                    // Reserva caso o push falhe: com o app aberto e disponível, consulta a cada 10 s.
                    LaunchedEffect(sessao != null, info.modo) {
                        if (sessao == null || info.modo != Modo.DISPONIVEL) return@LaunchedEffect
                        ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                            while (true) {
                                Corridas.sincronizarAgora()
                                delay(10_000)
                            }
                        }
                    }

                    BackHandler(enabled = noPerfil) { noPerfil = false }

                    val s = sessao
                    val o = oferta
                    val a = ativa
                    val conteudo = Modifier.safeDrawingPadding()
                    when {
                        s == null -> LoginScreen(conteudo)
                        o != null -> OfertaScreen(o, onFim = { }, modifier = conteudo)
                        noPerfil -> PerfilScreen(
                            email = s.email,
                            versao = BuildConfig.VERSION_NAME,
                            onVoltar = { noPerfil = false },
                            onSair = {
                                noPerfil = false
                                scope.launch { Tracking.sairDaConta() }
                            },
                            modifier = conteudo,
                            diagnostico = {
                                Linha("Estado do GPS", info.modo.rotulo)
                                Linha("Pontos na fila", info.naFila.toString())
                                Linha("Último erro", info.erro ?: "nenhum")
                            },
                        )
                        a != null -> CorridaScreen(a, admin = perfil?.admin == true, modifier = conteudo)
                        else -> TrackingScreen(
                            modifier = conteudo,
                            saudacao = NomePerfil(s.email).substringBefore(" "),
                            acoesTopo = { AvatarPerfil(s.email, onClick = { noPerfil = true }) },
                            extras = {
                                CartaoGanhos()
                                CartaoOfertas()
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (PwCore.auth.sessao.value != null) {
            Tracking.retomarSeAtivo()
            Corridas.sincronizar()
        }
    }
}
