package com.pwexpress.feature.corridas.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwCores
import com.pwexpress.core.designsystem.PwPonto
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.feature.corridas.data.Corrida
import com.pwexpress.feature.corridas.data.Corridas
import com.pwexpress.feature.corridas.data.Parada
import com.pwexpress.feature.corridas.data.StatusParada
import com.pwexpress.feature.corridas.data.TipoParada
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

/** Corrida em andamento: uma parada por vez, na ordem. */
@Composable
fun CorridaScreen(corrida: Corrida, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val parada = corrida.paradaAtual
    var enviando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var comprovanteAberto by remember { mutableStateOf(false) }

    fun avancar(acao: String) {
        val p = parada ?: return
        enviando = true
        erro = null
        scope.launch {
            Corridas.avancar(p.id, acao).onFailure { erro = it.message }
            enviando = false
        }
    }

    if (comprovanteAberto && parada != null) {
        ComprovanteEntrega(corrida, parada, onFechar = { comprovanteAberto = false }, modifier = modifier)
        return
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("CORRIDA #${corrida.numero}", color = PwCores.Ouro, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                corrida.empresaNome?.let { Text(it, color = PwCores.Cinza) }
            }
            Text(moeda(corrida.valor), fontWeight = FontWeight.Black, fontSize = 22.sp)
        }

        if (parada != null) {
            val coleta = parada.tipo == TipoParada.COLETA
            PwCartao(destaque = true) {
                PwRotulo("Parada ${parada.ordem + 1} de ${corrida.paradas.size} · ${if (coleta) "Coleta" else "Entrega"}")
                Spacer(Modifier.height(6.dp))
                Text(parada.endereco, style = MaterialTheme.typography.titleLarge)
                parada.contatoNome?.let { Text("Contato: $it", color = PwCores.Cinza) }
                parada.instrucoes?.let { Text(it, color = PwCores.OuroClaro) }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { abrirMaps(ctx, parada) }, modifier = Modifier.weight(1f)) { Text("Google Maps", color = PwCores.Branco) }
                    OutlinedButton(onClick = { abrirWaze(ctx, parada) }, modifier = Modifier.weight(1f)) { Text("Waze", color = PwCores.Branco) }
                    parada.contatoTelefone?.let { tel ->
                        OutlinedButton(onClick = { ligar(ctx, tel) }) { Text("Ligar", color = PwCores.Branco) }
                    }
                }
            }

            erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            val (rotulo, acao) = when {
                parada.status != StatusParada.CHEGOU -> "Cheguei no local" to { avancar("chegou") }
                coleta -> "Coletado ✓" to { avancar("concluir") }
                corrida.precisaComprovante -> "Finalizar entrega" to { comprovanteAberto = true }
                else -> "Entregue ✓" to { avancar("concluir") }
            }
            BotaoPrincipal(if (enviando) "Enviando…" else rotulo, habilitado = !enviando, onClick = acao)
        }

        PwCartao {
            PwRotulo("Trajeto")
            Spacer(Modifier.height(6.dp))
            corrida.paradas.forEach { p ->
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    PwPonto(
                        when {
                            p.status == StatusParada.CONCLUIDA -> PwCores.Verde
                            p.id == parada?.id -> PwCores.Ouro
                            else -> PwCores.Borda
                        },
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "${p.ordem + 1}. ${if (p.tipo == TipoParada.COLETA) "Coleta" else "Entrega"}" +
                                if (p.status == StatusParada.CONCLUIDA) " · concluída" else "",
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(p.endereco, color = PwCores.Cinza, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            corrida.observacoes?.let {
                Spacer(Modifier.height(6.dp))
                Text("Obs.: $it", color = PwCores.OuroClaro)
            }
        }
    }
}

private val Corrida.precisaComprovante get() = exigeNome || exigeFoto || exigeAssinatura || exigeCodigo

@Composable
private fun BotaoPrincipal(texto: String, habilitado: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PwCores.Ouro, contentColor = PwCores.Preto),
    ) { Text(texto, fontWeight = FontWeight.Black, fontSize = 19.sp) }
}

// ------------------------------------------------------------------ comprovante

@Composable
private fun ComprovanteEntrega(corrida: Corrida, parada: Parada, onFechar: () -> Unit, modifier: Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var nome by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var foto by remember { mutableStateOf<ByteArray?>(null) }
    var arquivoFoto by remember { mutableStateOf<File?>(null) }
    val assinatura = rememberEstadoAssinatura()
    var enviando by remember { mutableStateOf(false) }
    var etapa by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val arq = arquivoFoto
        if (ok && arq != null) {
            scope.launch {
                foto = runCatching { withContext(Dispatchers.Default) { Fotos.preparar(arq) } }
                    .onFailure { erro = "Não foi possível ler a foto." }.getOrNull()
                arq.delete()
            }
        }
    }

    fun finalizar() {
        erro = when {
            corrida.exigeNome && nome.isBlank() -> "Informe o nome de quem recebeu."
            corrida.exigeCodigo && codigo.length != 4 -> "Digite o código de 4 dígitos."
            corrida.exigeFoto && foto == null -> "Tire a foto do comprovante."
            corrida.exigeAssinatura && assinatura.vazia -> "Colete a assinatura."
            else -> null
        }
        if (erro != null) return
        enviando = true
        scope.launch {
            var fotoPath: String? = null
            var assinaturaPath: String? = null
            val f = foto
            if (corrida.exigeFoto && f != null) {
                etapa = "Enviando foto…"
                fotoPath = Corridas.enviarComprovante(corrida.id, parada.id, "foto", f, "image/jpeg")
                    .onFailure { erro = it.message }.getOrNull() ?: run { enviando = false; return@launch }
            }
            if (corrida.exigeAssinatura) {
                etapa = "Enviando assinatura…"
                val png = withContext(Dispatchers.Default) { assinatura.exportarPng() }
                assinaturaPath = Corridas.enviarComprovante(corrida.id, parada.id, "assinatura", png, "image/png")
                    .onFailure { erro = it.message }.getOrNull() ?: run { enviando = false; return@launch }
            }
            etapa = "Finalizando…"
            Corridas.avancar(parada.id, "concluir", buildJsonObject {
                put("recebedor_nome", nome.trim())
                put("codigo", codigo)
                fotoPath?.let { put("foto_path", it) }
                assinaturaPath?.let { put("assinatura_path", it) }
            }).onSuccess { onFechar() }.onFailure { erro = it.message }
            enviando = false
        }
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Comprovante de entrega", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onFechar, enabled = !enviando) { Text("Voltar", color = PwCores.Cinza) }
        }
        Text(parada.endereco, color = PwCores.Cinza)

        if (corrida.exigeNome) {
            OutlinedTextField(nome, { nome = it }, label = { Text("Nome de quem recebeu") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        if (corrida.exigeCodigo) {
            OutlinedTextField(
                codigo, { codigo = it.filter(Char::isDigit).take(4) }, label = { Text("Código de entrega (4 dígitos)") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (corrida.exigeFoto) {
            PwCartao {
                PwRotulo("Foto")
                Spacer(Modifier.height(8.dp))
                foto?.let { bytes ->
                    Fotos.miniatura(bytes)?.let {
                        Image(it.asImageBitmap(), "Foto do comprovante", contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)))
                        Spacer(Modifier.height(8.dp))
                    }
                }
                OutlinedButton(onClick = {
                    val (arq, uri) = Fotos.novoDestino(ctx)
                    arquivoFoto = arq
                    camera.launch(uri)
                }, modifier = Modifier.fillMaxWidth()) { Text(if (foto == null) "Tirar foto" else "Tirar outra", color = PwCores.Branco) }
            }
        }
        if (corrida.exigeAssinatura) {
            PwCartao {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PwRotulo("Assinatura", Modifier.weight(1f))
                    TextButton(onClick = { assinatura.limpar() }) { Text("Limpar", color = PwCores.Ouro) }
                }
                PainelAssinatura(assinatura)
            }
        }

        erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        BotaoPrincipal(if (enviando) etapa else "Confirmar entrega", habilitado = !enviando, onClick = { finalizar() })
        Spacer(Modifier.size(12.dp))
    }
}

// ------------------------------------------------------------------ navegação

private fun abrirMaps(ctx: Context, p: Parada) {
    val app = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${p.lat},${p.lng}"))
        .setPackage("com.google.android.apps.maps")
    val generico = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${p.lat},${p.lng}?q=${p.lat},${p.lng}"))
    runCatching { ctx.startActivity(app) }.recoverCatching { ctx.startActivity(generico) }
}

private fun abrirWaze(ctx: Context, p: Parada) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?ll=${p.lat},${p.lng}&navigate=yes"))) }
}

private fun ligar(ctx: Context, telefone: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${telefone.filter { it.isDigit() || it == '+' }}"))) }
}
