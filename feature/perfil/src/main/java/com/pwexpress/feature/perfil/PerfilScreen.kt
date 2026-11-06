package com.pwexpress.feature.perfil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.PreferenciaTema
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCabeca
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.core.perfil.Perfis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Meu perfil: nome, foto e tema. Contas gestor + motoboy veem também o modo teste
 * e o diagnóstico (o conteúdo do diagnóstico vem do :app, que conhece os outros módulos).
 */
@Composable
fun PerfilScreen(
    email: String,
    versao: String = "",
    onVoltar: () -> Unit,
    onSair: () -> Unit,
    modifier: Modifier = Modifier,
    diagnostico: @Composable () -> Unit = {},
) {
    val ctx = LocalContext.current
    val c = Pw.cores
    val scope = rememberCoroutineScope()
    val perfil by Perfis.perfil.collectAsState()
    val tema by TemaApp.preferencia.collectAsState()

    var nome by remember(perfil?.nome) { mutableStateOf(perfil?.nome.orEmpty()) }
    var novaFoto by remember { mutableStateOf<ByteArray?>(null) }
    var salvando by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var trocandoModo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { Perfis.carregar() }

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { novaFoto = withContext(Dispatchers.Default) { prepararFoto(ctx, uri) } }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) scope.launch { novaFoto = withContext(Dispatchers.Default) { quadrado(bmp) } }
    }

    fun salvar() {
        salvando = true
        msg = null
        scope.launch {
            val mudouNome = nome.trim().takeIf { it != perfil?.nome }
            Perfis.atualizar(mudouNome, novaFoto)
                .onSuccess { novaFoto = null; msg = true to "Perfil atualizado." }
                .onFailure { msg = false to (it.message ?: "Erro ao salvar.") }
            salvando = false
        }
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        com.pwexpress.core.designsystem.PwCabecalho("Editar perfil", onVoltar, subtitulo = email)

        // ---------------------------------------------------------- foto e nome
        PwCartao(destaque = true) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(132.dp).clip(CircleShape).background(c.gradOuro).padding(4.dp).clip(CircleShape).background(c.fundo2)
                        .clickable { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center,
                ) {
                    val bytes = novaFoto
                    when {
                        bytes != null -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let {
                            Image(it.asImageBitmap(), "Nova foto", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                        perfil?.fotoUrl != null -> AsyncImage(model = perfil?.fotoUrl, contentDescription = "Foto", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else -> Text(
                            (nome.ifBlank { email }).trim().take(1).uppercase(),
                            style = TextStyle(brush = c.gradOuro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 52.sp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PwBotaoVidro("Galeria", onClick = { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, icone = PwIcones.Image)
                    PwBotaoVidro("Câmera", onClick = { camera.launch(null) }, icone = PwIcones.Camera)
                }
            }
            Spacer(Modifier.height(14.dp))
            com.pwexpress.core.designsystem.PwCampo(nome, { nome = it.take(60) }, "Nome de exibição")
            Text("Aparece no mapa, nas corridas e no site (é a mesma conta).", color = c.texto3, style = MaterialTheme.typography.bodyMedium)
            msg?.let { (ok, texto) -> Text(texto, color = if (ok) c.verde else c.vermelho, modifier = Modifier.padding(top = 6.dp)) }
            Spacer(Modifier.height(10.dp))
            val mudou = novaFoto != null || (nome.trim().isNotEmpty() && nome.trim() != perfil?.nome)
            PwBotaoOuro(if (salvando) "Salvando…" else "Salvar", onClick = { salvar() }, icone = PwIcones.Check, habilitado = mudou && !salvando)
        }

        // ---------------------------------------------------------- diagnóstico (administrador)
        if (perfil?.admin == true) {
            PwCartao {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(PwIcones.Bug, null, tint = c.ouro, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    PwRotulo("Diagnóstico")
                }
                Spacer(Modifier.height(8.dp))
                perfil?.configuracoes?.forEach { (k, v) -> Linha(k, v.toString().trim('"')) }
                diagnostico()
            }
        }

    }
}

@Composable
private fun OpcaoTema(rotulo: String, icone: androidx.compose.ui.graphics.vector.ImageVector, ativo: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Pw.cores
    val forma = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    Column(
        modifier.clip(forma)
            .background(if (ativo) c.gradSuave else androidx.compose.ui.graphics.SolidColor(c.vidroSutil))
            .border(1.dp, if (ativo) c.ouro else c.borda, forma)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icone, null, tint = if (ativo) c.ouro else c.texto2, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(rotulo, color = if (ativo) c.ouro else c.texto2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Linha do diagnóstico (pública para o :app acrescentar itens). */
@Composable
fun Linha(rotulo: String, valor: String) {
    val c = Pw.cores
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(rotulo, Modifier.weight(1f), color = c.texto2, style = MaterialTheme.typography.bodyMedium)
        Text(valor, color = c.texto, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

// ------------------------------------------------------------------ foto

private fun prepararFoto(ctx: Context, uri: Uri): ByteArray? = runCatching {
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
    var amostra = 1
    while (minOf(limites.outWidth, limites.outHeight) / (amostra * 2) >= 512) amostra *= 2
    val bmp = ctx.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = amostra })
    } ?: return null
    quadrado(bmp)
}.getOrNull()

/** Recorta no centro, reduz para 512 px e comprime. */
private fun quadrado(bmp: Bitmap): ByteArray {
    val lado = minOf(bmp.width, bmp.height)
    val recorte = Bitmap.createBitmap(bmp, (bmp.width - lado) / 2, (bmp.height - lado) / 2, lado, lado)
    val final = if (lado > 512) Bitmap.createScaledBitmap(recorte, 512, 512, true) else recorte
    return ByteArrayOutputStream().also { final.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
}
