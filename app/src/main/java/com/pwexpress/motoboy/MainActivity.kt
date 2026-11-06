package com.pwexpress.motoboy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pwexpress.core.PwCore
import com.pwexpress.core.designsystem.PwTheme
import com.pwexpress.feature.auth.LoginScreen
import com.pwexpress.feature.corridas.data.Corridas
import com.pwexpress.feature.corridas.ui.CartaoOfertas
import com.pwexpress.feature.corridas.ui.CorridaScreen
import com.pwexpress.feature.corridas.ui.OfertaScreen
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
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val sessao by PwCore.auth.sessao.collectAsState()
                    val oferta by Corridas.oferta.collectAsState()
                    val ativa by Corridas.ativa.collectAsState()
                    val info by Tracking.info.collectAsState()
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

                    val s = sessao
                    val o = oferta
                    val a = ativa
                    val conteudo = Modifier.safeDrawingPadding()
                    when {
                        s == null -> LoginScreen(conteudo)
                        o != null -> OfertaScreen(o, onFim = { }, modifier = conteudo)
                        a != null -> CorridaScreen(a, modifier = conteudo)
                        else -> TrackingScreen(
                            email = s.email,
                            onSair = { scope.launch { Tracking.sairDaConta() } },
                            modifier = conteudo,
                            extras = { CartaoOfertas() },
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
