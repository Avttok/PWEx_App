package com.pwexpress.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Marca textual: "PW" dourado + "EXPRESS" branco. */
@Composable
fun PwLogo(modifier: Modifier = Modifier, tamanho: Int = 28) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("PW", color = PwCores.Ouro, fontWeight = FontWeight.Black, fontSize = tamanho.sp)
        Text(
            " EXPRESS",
            color = PwCores.Branco,
            fontWeight = FontWeight.Light,
            fontSize = (tamanho * 0.62f).sp,
            letterSpacing = (tamanho * 0.12f).sp,
        )
    }
}

/** Cartão padrão: grafite com borda sutil. */
@Composable
fun PwCartao(
    modifier: Modifier = Modifier,
    destaque: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val forma = RoundedCornerShape(18.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(forma)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, if (destaque) PwCores.Ouro else PwCores.Borda, forma)
            .padding(18.dp),
        content = content,
    )
}

@Composable
fun PwPonto(cor: Color, tamanho: Dp = 12.dp) {
    Box(Modifier.size(tamanho).clip(CircleShape).background(cor))
}

@Composable
fun PwRotulo(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
