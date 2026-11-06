package com.pwexpress.motoboy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.PreferenciaTema
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwAvatarConta
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.core.perfil.Perfis
import com.pwexpress.core.update.Atualizador
import kotlinx.coroutines.launch

/** Qual lado do app está aberto (para quem é gestor e motoboy). */
enum class Painel { MOTOBOY, GESTOR }

/** Ações do topo: troca motoboy/gestor (quando tem os dois) e a foto, único caminho para o menu da conta. */
@Composable
fun RowScope.AcoesTopo(painel: Painel, podeTrocar: Boolean, onTrocar: (Painel) -> Unit, onConta: () -> Unit) {
    val c = Pw.cores
    val perfil by Perfis.perfil.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    if (podeTrocar) {
        val forma = RoundedCornerShape(99.dp)
        Row(Modifier.clip(forma).background(c.vidroForte).border(1.dp, c.borda, forma).padding(3.dp)) {
            listOf(Painel.MOTOBOY to PwIcones.Bike, Painel.GESTOR to PwIcones.ShieldCheck).forEach { (p, i) ->
                val ativo = p == painel
                Box(
                    Modifier.clip(forma).then(if (ativo) Modifier.background(c.gradOuro) else Modifier).clickable { onTrocar(p) }.padding(horizontal = 11.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) { Icon(i, if (p == Painel.MOTOBOY) "Lado do motoboy" else "Painel do gestor", tint = if (ativo) c.sobreOuro else c.texto3, modifier = Modifier.size(17.dp)) }
            }
        }
    }
    PwAvatarConta(perfil?.nome ?: "", perfil?.fotoUrl, teste, tamanho = 42.dp, onClick = onConta)
}

/**
 * Menu da conta, aberto pela foto (como no site): perfil, tema, modo teste,
 * configurações (gestor), trocar de lado, atualização e sair.
 */
@Composable
fun JanelaConta(
    email: String,
    painel: Painel,
    podeTrocar: Boolean,
    onFechar: () -> Unit,
    onPerfil: () -> Unit,
    onConfiguracoes: () -> Unit,
    onTrocar: (Painel) -> Unit,
    onSair: () -> Unit,
) {
    val c = Pw.cores
    val perfil by Perfis.perfil.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    var erro by remember { mutableStateOf<String?>(null) }
    var trocandoTeste by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()

    Dialog(onDismissRequest = onFechar, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(
            Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onFechar),
        ) {
            val forma = RoundedCornerShape(24.dp)
            Column(
                Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 62.dp, end = 12.dp, start = 12.dp).widthIn(max = 340.dp).fillMaxWidth()
                    .clip(forma).background(c.fundo2).border(1.dp, c.bordaForte, forma)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                    .verticalScroll(rememberScrollState()).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // cabeça: foto, nome e e-mail (toque edita o perfil)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onPerfil).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PwAvatarConta(perfil?.nome ?: email, perfil?.fotoUrl, teste, tamanho = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(perfil?.nome ?: email.substringBefore("@"), color = c.texto, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(email, color = c.texto3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(c.borda))

                ItemMenu(PwIcones.UserRoundPen, "Editar perfil", onClick = onPerfil)
                val escuro = c.escuro
                ItemMenu(if (escuro) PwIcones.Moon else PwIcones.Sun, if (escuro) "Tema noturno" else "Tema claro",
                    onClick = { TemaApp.definir(if (escuro) PreferenciaTema.CLARO else PreferenciaTema.ESCURO) }) {
                    Interruptor(escuro) { TemaApp.definir(if (it) PreferenciaTema.ESCURO else PreferenciaTema.CLARO) }
                }
                if (perfil?.gestor == true) {
                    fun alternarTeste(lig: Boolean) {
                        if (trocandoTeste) return
                        trocandoTeste = true
                        escopo.launch { Perfis.definirModoTeste(lig).onFailure { erro = it.message }.onSuccess { erro = null }; trocandoTeste = false }
                    }
                    ItemMenu(PwIcones.FlaskConical, "Modo teste", subtitulo = if (teste) "Só corridas e ganhos de teste" else null, cor = c.violeta, onClick = { alternarTeste(!teste) }) {
                        Interruptor(teste, c.violeta) { alternarTeste(it) }
                    }
                }
                PwErro(erro)
                if (painel == Painel.GESTOR) ItemMenu(PwIcones.Settings2, "Configurações", onClick = onConfiguracoes)
                if (podeTrocar) {
                    if (painel == Painel.MOTOBOY) ItemMenu(PwIcones.ShieldCheck, "Painel do gestor", onClick = { onTrocar(Painel.GESTOR) })
                    else ItemMenu(PwIcones.Bike, "Lado do motoboy", onClick = { onTrocar(Painel.MOTOBOY) })
                }
                ItemMenu(PwIcones.Download, "Verificar atualização", onClick = { onFechar(); Atualizador.abrirJanela(); Atualizador.verificar(automatico = false) })
                Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(c.borda))
                ItemMenu(PwIcones.LogOut, "Sair da conta", cor = c.vermelho, corTexto = c.vermelho, onClick = onSair)
            }
        }
    }
}

@Composable
private fun ItemMenu(
    icone: ImageVector,
    texto: String,
    subtitulo: String? = null,
    cor: Color? = null,
    corTexto: Color? = null,
    onClick: () -> Unit,
    direita: @Composable RowScope.() -> Unit = {},
) {
    val c = Pw.cores
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icone, null, tint = cor ?: c.ouro, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(texto, color = corTexto ?: c.texto, fontWeight = FontWeight.SemiBold, fontFamily = Archivo, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitulo != null) Text(subtitulo, color = c.texto3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = direita)
    }
}

@Composable
private fun Interruptor(ligado: Boolean, cor: Color? = null, onMudar: (Boolean) -> Unit) {
    val c = Pw.cores
    Switch(
        checked = ligado, onCheckedChange = onMudar,
        colors = SwitchDefaults.colors(checkedThumbColor = c.sobreOuro, checkedTrackColor = cor ?: c.ouro, uncheckedTrackColor = c.bordaForte, uncheckedBorderColor = Color.Transparent),
    )
}
