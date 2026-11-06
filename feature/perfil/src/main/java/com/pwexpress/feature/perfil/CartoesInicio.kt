package com.pwexpress.feature.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.perfil.Perfis
import java.text.NumberFormat
import java.util.Locale

private val br = Locale.forLanguageTag("pt-BR")
private fun moeda(v: Double) = NumberFormat.getCurrencyInstance(br).format(v)

/** Avatar redondo com anel dourado (foto ou inicial). Toque abre o perfil. */
@Composable
fun AvatarPerfil(email: String, onClick: () -> Unit, tamanho: Int = 46) {
    val c = Pw.cores
    val perfil by Perfis.perfil.collectAsState()
    Box(
        Modifier.size(tamanho.dp).clip(CircleShape).background(c.gradOuro).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size((tamanho - 5).dp).clip(CircleShape).background(c.fundo2), contentAlignment = Alignment.Center) {
            val foto = perfil?.fotoUrl
            if (foto != null) {
                AsyncImage(model = foto, contentDescription = "Meu perfil", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(
                    (perfil?.nome?.ifBlank { null } ?: email).trim().take(1).uppercase(),
                    style = TextStyle(brush = c.gradOuro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = (tamanho * 0.42f).sp),
                )
            }
        }
    }
}

/** Nome de exibição (ou o e-mail enquanto carrega). */
@Composable
fun NomePerfil(email: String): String {
    val perfil by Perfis.perfil.collectAsState()
    return perfil?.nome?.ifBlank { null } ?: email.substringBefore("@")
}

/** Ganhos do dia, semana e mês (só corridas reais concluídas). */
@Composable
fun CartaoGanhos() {
    val c = Pw.cores
    val ganhos by Perfis.ganhos.collectAsState()
    val perfil by Perfis.perfil.collectAsState()
    LaunchedEffect(Unit) { Perfis.carregar() }

    PwCartao(destaque = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PwRotulo("Seus ganhos", Modifier.weight(1f))
            if (perfil?.modoTeste == true) PwSelo("modo teste", c.violeta, icone = PwIcones.FlaskConical)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            moeda(ganhos?.hoje ?: 0.0),
            style = TextStyle(brush = c.gradOuro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 38.sp, letterSpacing = (-0.8).sp),
        )
        Text(
            "hoje · ${ganhos?.corridasHoje ?: 0} corrida${if ((ganhos?.corridasHoje ?: 0) == 1) "" else "s"}",
            color = c.texto2, fontFamily = Archivo, fontSize = 13.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Mini("Semana", moeda(ganhos?.semana ?: 0.0), Modifier.weight(1f))
            Mini("Mês", moeda(ganhos?.mes ?: 0.0), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Mini(rotulo: String, valor: String, modifier: Modifier) {
    val c = Pw.cores
    val forma = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    Column(modifier.clip(forma).background(c.vidroSutil).border(1.dp, c.borda, forma).padding(12.dp)) {
        PwRotulo(rotulo)
        Spacer(Modifier.width(2.dp))
        Text(valor, color = c.texto, fontFamily = ArchivoLargo, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

