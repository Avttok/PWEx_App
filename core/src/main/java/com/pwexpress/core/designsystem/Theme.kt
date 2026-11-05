package com.pwexpress.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Paleta PW Express: dourado, preto e branco. */
object PwCores {
    val Ouro = Color(0xFFD4AF37)
    val OuroClaro = Color(0xFFE9C766)
    val OuroEscuro = Color(0xFF8C7322)
    val Preto = Color(0xFF0A0A0A)
    val Grafite = Color(0xFF151515)
    val Grafite2 = Color(0xFF1F1F1F)
    val Borda = Color(0xFF2C2C2C)
    val Branco = Color(0xFFFFFFFF)
    val Cinza = Color(0xFF9A9A9A)
    val Verde = Color(0xFF2ECC71)
    val Vermelho = Color(0xFFE5484D)
}

private val esquema = darkColorScheme(
    primary = PwCores.Ouro,
    onPrimary = PwCores.Preto,
    primaryContainer = PwCores.OuroEscuro,
    onPrimaryContainer = PwCores.Branco,
    secondary = PwCores.OuroClaro,
    onSecondary = PwCores.Preto,
    background = PwCores.Preto,
    onBackground = PwCores.Branco,
    surface = PwCores.Grafite,
    onSurface = PwCores.Branco,
    surfaceVariant = PwCores.Grafite2,
    onSurfaceVariant = PwCores.Cinza,
    outline = PwCores.Borda,
    error = PwCores.Vermelho,
    onError = PwCores.Branco,
)

private val tipografia = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = 0.5.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.2.sp),
)

@Composable
fun PwTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = esquema, typography = tipografia, content = content)
}
