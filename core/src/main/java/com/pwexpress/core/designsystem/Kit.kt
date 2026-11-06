package com.pwexpress.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.util.Locale

// =====================================================================
// Kit de componentes do app (mesma linguagem do site).
// =====================================================================

private val br = Locale.forLanguageTag("pt-BR")
fun moedaBr(v: Double?): String = if (v == null || v.isNaN()) "—" else NumberFormat.getCurrencyInstance(br).format(v)
fun numeroBr(v: Double, casas: Int = 2): String = NumberFormat.getNumberInstance(br).apply { maximumFractionDigits = casas }.format(v)

// ------------------------------------------------------------------ topo e abas

/** Topo das telas principais: logo + "PW Express" e ações à direita. */
@Composable
fun PwTopo(modifier: Modifier = Modifier, acoes: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        PwLogo(tamanho = 22)
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), content = acoes)
    }
}

data class PwAba(val chave: String, val rotulo: String, val icone: ImageVector, val contador: Int = 0)

/** Barra de abas flutuante (vidro), como uma pílula no rodapé. */
@Composable
fun PwBarraAbas(abas: List<PwAba>, selecionada: String, onSelecionar: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = Pw.cores
    val forma = RoundedCornerShape(26.dp)
    Row(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .fillMaxWidth()
            .clip(forma)
            .background(c.vidroForte)
            .border(1.dp, c.bordaForte, forma)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        abas.forEach { aba ->
            val ativa = aba.chave == selecionada
            val cor by animateColorAsState(if (ativa) c.ouro else c.texto3, label = "cor-aba")
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .then(if (ativa) Modifier.background(c.gradSuave) else Modifier)
                    .clickable { onSelecionar(aba.chave) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box {
                    Icon(aba.icone, null, tint = cor, modifier = Modifier.size(22.dp))
                    if (aba.contador > 0) {
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(start = 14.dp).size(16.dp).clip(CircleShape).background(c.ouro),
                            contentAlignment = Alignment.Center,
                        ) { Text(if (aba.contador > 9) "9+" else aba.contador.toString(), color = c.sobreOuro, fontSize = 9.sp, fontWeight = FontWeight.Black) }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(aba.rotulo, color = cor, fontSize = 11.sp, fontWeight = if (ativa) FontWeight.Bold else FontWeight.Medium, fontFamily = Archivo, maxLines = 1)
            }
        }
    }
}

// ------------------------------------------------------------------ seleção

/** Botões lado a lado (um selecionado). */
@Composable
fun PwSegmentado(opcoes: List<Pair<String, ImageVector?>>, selecionado: Int, onSelecionar: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Pw.cores
    val forma = RoundedCornerShape(14.dp)
    Row(modifier.fillMaxWidth().clip(forma).background(c.vidroSutil).border(1.dp, c.borda, forma).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        opcoes.forEachIndexed { i, (rotulo, icone) ->
            val ativo = i == selecionado
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).then(if (ativo) Modifier.background(c.vidroForte) else Modifier)
                    .clickable { onSelecionar(i) }.padding(vertical = 10.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icone != null) {
                    Icon(icone, null, tint = if (ativo) c.ouro else c.texto2, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(rotulo, color = if (ativo) c.ouro else c.texto2, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, fontFamily = Archivo, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Filtros em fichas que rolam para o lado (com contador opcional). */
@Composable
fun PwFiltros(opcoes: List<Pair<String, Int>>, selecionado: Int, onSelecionar: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Pw.cores
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        opcoes.forEachIndexed { i, (rotulo, n) ->
            val ativo = i == selecionado
            val forma = RoundedCornerShape(99.dp)
            Row(
                Modifier.clip(forma)
                    .background(if (ativo) c.gradOuro else Brush.linearGradient(listOf(c.vidroForte, c.vidroForte)))
                    .border(1.dp, if (ativo) Color.Transparent else c.borda, forma)
                    .clickable { onSelecionar(i) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(rotulo, color = if (ativo) c.sobreOuro else c.texto2, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, fontFamily = Archivo)
                if (n > 0) {
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.clip(CircleShape).background(if (ativo) c.sobreOuro.copy(alpha = 0.2f) else c.ouro).padding(horizontal = 6.dp, vertical = 1.dp)) {
                        Text(n.toString(), color = if (ativo) c.sobreOuro else c.sobreOuro, fontSize = 10.5.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

/** Campo de escolha (abre uma lista). */
@Composable
fun <T> PwSelecao(rotulo: String, opcoes: List<Pair<T, String>>, valor: T?, onMudar: (T) -> Unit, modifier: Modifier = Modifier, vazio: String = "Escolha…") {
    val c = Pw.cores
    var aberto by remember { mutableStateOf(false) }
    Column(modifier) {
        if (rotulo.isNotEmpty()) { PwRotulo(rotulo); Spacer(Modifier.height(6.dp)) }
        Box {
            val forma = RoundedCornerShape(14.dp)
            Row(
                Modifier.fillMaxWidth().clip(forma).background(c.campo).border(1.dp, c.bordaForte, forma).clickable { aberto = true }.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(opcoes.firstOrNull { it.first == valor }?.second ?: vazio, color = if (valor == null) c.texto3 else c.texto, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(PwIcones.ChevronDown, null, tint = c.texto3, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }, modifier = Modifier.background(c.fundo2)) {
                opcoes.forEach { (v, texto) ->
                    DropdownMenuItem(
                        text = { Text(texto, color = if (v == valor) c.ouro else c.texto, fontWeight = if (v == valor) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { onMudar(v); aberto = false },
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ campos

@Composable
fun PwCampo(
    valor: String,
    onMudar: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    dica: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    senha: Boolean = false,
    linhas: Int = 1,
    prefixo: String? = null,
    sufixo: String? = null,
    habilitado: Boolean = true,
) {
    val c = Pw.cores
    OutlinedTextField(
        value = valor,
        onValueChange = onMudar,
        label = { Text(rotulo) },
        placeholder = dica?.let { { Text(it, color = c.texto3) } },
        singleLine = linhas == 1,
        minLines = linhas,
        enabled = habilitado,
        prefix = prefixo?.let { { Text(it, color = c.texto3) } },
        suffix = sufixo?.let { { Text(it, color = c.texto3) } },
        visualTransformation = if (senha) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.ouro, unfocusedBorderColor = c.bordaForte, focusedLabelColor = c.ouro, cursorColor = c.ouro,
            focusedContainerColor = c.campo, unfocusedContainerColor = c.campo, disabledContainerColor = c.campo,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Linha com interruptor (o toque na linha inteira liga/desliga). */
@Composable
fun PwInterruptor(titulo: String, ligado: Boolean, onMudar: (Boolean) -> Unit, modifier: Modifier = Modifier, descricao: String? = null, icone: ImageVector? = null, cor: Color? = null) {
    val c = Pw.cores
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onMudar(!ligado) }.padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icone != null) {
            Icon(icone, null, tint = cor ?: c.ouro, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, color = c.texto, fontWeight = FontWeight.SemiBold, fontFamily = Archivo, fontSize = 14.5.sp)
            if (descricao != null) Text(descricao, color = c.texto3, fontSize = 12.5.sp, lineHeight = 17.sp)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = ligado, onCheckedChange = onMudar,
            colors = SwitchDefaults.colors(checkedThumbColor = c.sobreOuro, checkedTrackColor = cor ?: c.ouro, uncheckedTrackColor = c.bordaForte, uncheckedBorderColor = Color.Transparent),
        )
    }
}

/** Número com botões − e + (ajustes de configuração). */
@Composable
fun PwPasso(valor: Double, onMudar: (Double) -> Unit, modifier: Modifier = Modifier, passo: Double = 1.0, min: Double = 0.0, max: Double = 1e9, unidade: String? = null) {
    val c = Pw.cores
    val forma = RoundedCornerShape(14.dp)
    fun ajustar(d: Double) = onMudar(((valor + d) * 1000).let { Math.round(it) / 1000.0 }.coerceIn(min, max))
    Row(modifier.clip(forma).background(c.campo).border(1.dp, c.bordaForte, forma), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clickable(enabled = valor > min) { ajustar(-passo) }, contentAlignment = Alignment.Center) {
            Icon(PwIcones.Minus, "Diminuir", tint = if (valor > min) c.ouro else c.texto3, modifier = Modifier.size(18.dp))
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (unidade == "R$") Text("R$ ", color = c.texto3, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(numeroBr(valor, 2), color = c.texto, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = Archivo)
            if (unidade != null && unidade != "R$") Text(" $unidade", color = c.texto3, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.size(48.dp).clickable(enabled = valor < max) { ajustar(passo) }, contentAlignment = Alignment.Center) {
            Icon(PwIcones.Plus, "Aumentar", tint = if (valor < max) c.ouro else c.texto3, modifier = Modifier.size(18.dp))
        }
    }
}

// ------------------------------------------------------------------ listas e estados

/** Linha tocável de lista (ícone, título, subtítulo e algo à direita). */
@Composable
fun PwLinha(titulo: String, modifier: Modifier = Modifier, subtitulo: String? = null, icone: ImageVector? = null, corIcone: Color? = null, onClick: (() -> Unit)? = null, direita: @Composable RowScope.() -> Unit = {}) {
    val c = Pw.cores
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icone != null) {
            PwAzulejo(icone, tamanho = 38.dp, cor = corIcone)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, color = c.texto, fontWeight = FontWeight.SemiBold, fontFamily = Archivo, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitulo != null) Text(subtitulo, color = c.texto3, fontSize = 12.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = direita)
        if (onClick != null) Icon(PwIcones.ChevronRight, null, tint = c.texto3, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun PwVazio(texto: String, modifier: Modifier = Modifier, icone: ImageVector = PwIcones.Info) {
    val c = Pw.cores
    Column(modifier.fillMaxWidth().padding(vertical = 34.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PwAzulejo(icone, tamanho = 52.dp)
        Spacer(Modifier.height(12.dp))
        Text(texto, color = c.texto2, textAlign = TextAlign.Center, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
fun PwCarregando(modifier: Modifier = Modifier, texto: String? = null) {
    val c = Pw.cores
    Column(modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = c.ouro, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
        if (texto != null) { Spacer(Modifier.height(10.dp)); Text(texto, color = c.texto3, fontSize = 13.sp) }
    }
}

/** Mensagem de erro em caixa vermelha suave. */
@Composable
fun PwErro(texto: String?, modifier: Modifier = Modifier) {
    if (texto.isNullOrBlank()) return
    val c = Pw.cores
    val forma = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().clip(forma).background(c.vermelho.copy(alpha = 0.10f)).border(1.dp, c.vermelho.copy(alpha = 0.35f), forma).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(PwIcones.Alerta, null, tint = c.vermelho, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(10.dp))
        Text(texto, color = c.texto, fontSize = 13.5.sp, lineHeight = 19.sp)
    }
}

/** Título de seção com ação opcional à direita. */
@Composable
fun PwSecao(titulo: String, modifier: Modifier = Modifier, acao: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        PwRotulo(titulo, Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, content = acao)
    }
}

/** Botão de texto vermelho (ações perigosas). */
@Composable
fun PwBotaoPerigo(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, icone: ImageVector? = PwIcones.Trash2, habilitado: Boolean = true, cheio: Boolean = false) {
    val c = Pw.cores
    val forma = RoundedCornerShape(16.dp)
    Row(
        modifier.heightIn(min = 50.dp).clip(forma)
            .background(if (cheio) c.vermelho else c.vermelho.copy(alpha = 0.08f))
            .border(1.dp, c.vermelho.copy(alpha = if (cheio) 0f else 0.5f), forma)
            .clickable(enabled = habilitado, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        val cor = if (cheio) Color.White else c.vermelho
        if (icone != null) { Icon(icone, null, tint = cor.copy(alpha = if (habilitado) 1f else 0.5f), modifier = Modifier.size(17.dp)); Spacer(Modifier.width(8.dp)) }
        Text(texto, color = cor.copy(alpha = if (habilitado) 1f else 0.5f), fontWeight = FontWeight.Bold, fontFamily = Archivo)
    }
}

// ------------------------------------------------------------------ avatar

/** Foto (ou inicial) com anel dourado; `teste` troca o anel pelo violeta. */
@Composable
fun PwAvatar(nome: String, foto: String?, modifier: Modifier = Modifier, tamanho: Dp = 44.dp, onClick: (() -> Unit)? = null) {
    val c = Pw.cores
    Box(
        modifier.size(tamanho).clip(CircleShape).background(c.gradOuro).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(tamanho - 4.dp).clip(CircleShape).background(c.fundo2), contentAlignment = Alignment.Center) {
            if (!foto.isNullOrBlank()) {
                AsyncImage(model = foto, contentDescription = nome, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(
                    nome.trim().take(1).uppercase().ifEmpty { "?" },
                    style = TextStyle(brush = c.gradOuro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = (tamanho.value * 0.42f).sp),
                )
            }
        }
    }
}

// ------------------------------------------------------------------ janelas

/** Janela centralizada de vidro (fundo sólido para ler bem). */
@Composable
fun PwJanela(titulo: String, onFechar: () -> Unit, modifier: Modifier = Modifier, fecharFora: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val c = Pw.cores
    Dialog(onDismissRequest = { if (fecharFora) onFechar() }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = fecharFora, dismissOnBackPress = fecharFora)) {
        val forma = RoundedCornerShape(26.dp)
        Column(
            modifier.padding(16.dp).widthIn(max = 520.dp).fillMaxWidth().clip(forma).background(c.fundo2).border(1.dp, c.bordaForte, forma)
                .verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, color = c.texto, modifier = Modifier.weight(1f))
                if (fecharFora) PwBotaoIcone(PwIcones.X, "Fechar", onFechar)
            }
            content()
        }
    }
}

/** Botão flutuante redondo dourado (ex.: nova corrida). */
@Composable
fun PwBotaoFlutuante(icone: ImageVector, texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Pw.cores
    val forma = RoundedCornerShape(20.dp)
    Row(
        modifier.clip(forma).background(c.gradOuro).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icone, null, tint = c.sobreOuro, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, color = c.sobreOuro, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 15.sp)
    }
}

/** Cartão-número (painéis de resumo). */
@Composable
fun PwNumero(rotulo: String, valor: String, modifier: Modifier = Modifier, icone: ImageVector? = null, destaque: Boolean = false) {
    val c = Pw.cores
    val forma = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(forma)
            .background(if (destaque) Brush.linearGradient(listOf(c.ouro.copy(alpha = 0.16f), Color.Transparent)) else Brush.linearGradient(listOf(c.vidroSutil, c.vidroSutil)))
            .border(1.dp, if (destaque) c.ouro.copy(alpha = 0.35f) else c.borda, forma)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icone != null) { Icon(icone, null, tint = c.texto3, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(4.dp)) }
            PwRotulo(rotulo)
        }
        Spacer(Modifier.height(4.dp))
        if (destaque) Text(valor, style = TextStyle(brush = c.gradOuro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 24.sp), maxLines = 1)
        else Text(valor, color = c.texto, fontFamily = ArchivoLargo, fontWeight = FontWeight.Bold, fontSize = 19.sp, maxLines = 1)
    }
}

/** Some/aparece com fade (atalho). */
@Composable
fun PwSumir(visivel: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(visivel, enter = fadeIn(), exit = fadeOut()) { content() }
}

/** Escala suave ao pressionar não é trivial sem interação; usamos para destacar seleção. */
@Composable
fun Modifier.pwDestaque(ativo: Boolean): Modifier {
    val s by animateFloatAsState(if (ativo) 1f else 0.98f, label = "destaque")
    return this.scale(s)
}

/** Conteúdo rolável com espaçamento padrão das telas. */
@Composable
fun PwPagina(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp), content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}
