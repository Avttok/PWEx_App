package com.pwexpress.motoboy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.pwexpress.core.PwCore
import com.pwexpress.core.designsystem.PwTheme
import com.pwexpress.feature.auth.LoginScreen
import com.pwexpress.feature.tracking.Tracking
import com.pwexpress.feature.tracking.ui.TrackingScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PwTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val sessao by PwCore.auth.sessao.collectAsState()
                    val scope = rememberCoroutineScope()
                    val atual = sessao
                    if (atual == null) {
                        LoginScreen(Modifier.safeDrawingPadding())
                    } else {
                        TrackingScreen(
                            email = atual.email,
                            onSair = { scope.launch { Tracking.sairDaConta() } },
                            modifier = Modifier.safeDrawingPadding(),
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (PwCore.auth.sessao.value != null) Tracking.retomarSeAtivo()
    }
}
