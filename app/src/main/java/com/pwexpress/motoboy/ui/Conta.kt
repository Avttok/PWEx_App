package com.pwexpress.motoboy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.PreferenciaTema
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwAvatar
import com.pwexpress.core.designsystem.PwBotaoPerigo
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwLinha
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSegmentado
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.core.perfil.Perfis
import com.pwexpress.core.update.Atualizador
import kotlinx.coroutines.launch

/** Qual lado do app está aberto (para quem é gestor e motoboy). */
enum class Painel { MOTOBOY, GESTOR }

/** Ações do topo: troca motoboy/gestor (quando tem os dois) e a foto, que abre o menu da conta. */
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
                Row(
                    Modifier.clip(forma).then(if (ativo) Modifier.background(c.gradOuro) else Modifier).clickable { onTrocar(p) }.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) { Icon(i, if (p == Painel.MOTOBOY) "Motoboy" else "Gestor", tint = if (ativo) c.sobreOuro else c.texto3, modifier = Modifier.size(17.dp)) }
            }
        }
    }
    val anel = if (teste) Modifier.border(2.dp, c.violeta, RoundedCornerShape(99.dp)).padding(2.dp) else Modifier
    PwAvatar(perfil?.nome ?: "", perfil?.fotoUrl, anel, tamanho = 42.dp, onClick = onConta)
}

/** Menu da conta (mesmo do site): perfil, tema, modo teste, atualização, trocar de lado e sair. */
@Composable
fun JanelaConta(
    email: String,
    painel: Painel,
    podeTrocar: Boolean,
    onFechar: () -> Unit,
    onPerfil: () -> Unit,
    onTrocar: (Painel) -> Unit,
    onSair: () -> Unit,
) {
    PwJanela("Minha conta", onFechar = onFechar) { ItensConta(email, painel, podeTrocar, onPerfil, onTrocar, onSair) }
}

@Composable
fun ColumnScope.ItensConta(
    email: String,
    painel: Painel,
    podeTrocar: Boolean,
    onPerfil: () -> Unit,
    onTrocar: (Painel) -> Unit,
    onSair: () -> Unit,
) {
    val c = Pw.cores
    val perfil by Perfis.perfil.collectAsState()
    val tema by TemaApp.preferencia.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    var erro by remember { mutableStateOf<String?>(null) }
    val escopo = rememberCoroutineScope()

    PwCartao(padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PwAvatar(perfil?.nome ?: email, perfil?.fotoUrl, tamanho = 54.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(perfil?.nome ?: email.substringBefore("@"), color = c.texto, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 17.sp)
                Text(email, color = c.texto3, fontSize = 12.5.sp)
            }
        }
        Spacer(Modifier.size(6.dp))
        PwLinha("Editar perfil", subtitulo = "Nome e foto (valem também no site)", icone = PwIcones.UserRoundPen, onClick = onPerfil)
    }
    PwCartao(padding = PaddingValues(14.dp)) {
        PwRotulo("Tema")
        Spacer(Modifier.size(8.dp))
        val opcoes = listOf(PreferenciaTema.SISTEMA, PreferenciaTema.CLARO, PreferenciaTema.ESCURO)
        PwSegmentado(listOf("Auto" to PwIcones.Monitor, "Claro" to PwIcones.Sun, "Noturno" to PwIcones.Moon), opcoes.indexOf(tema), { TemaApp.definir(opcoes[it]) })
        if (perfil?.gestor == true) {
            Spacer(Modifier.size(6.dp))
            PwInterruptor(
                "Modo teste", teste, { lig -> escopo.launch { Perfis.definirModoTeste(lig).onFailure { erro = it.message }.onSuccess { erro = null } } },
                descricao = if (teste) "App e painel em violeta: só corridas e ganhos de teste." else "Ver e criar só corridas de teste.",
                icone = PwIcones.FlaskConical, cor = c.violeta,
            )
        }
        PwErro(erro)
    }
    PwCartao(padding = PaddingValues(8.dp)) {
        if (podeTrocar) PwLinha(
            if (painel == Painel.MOTOBOY) "Abrir o painel do gestor" else "Voltar para o app do motoboy",
            subtitulo = if (painel == Painel.MOTOBOY) "Corridas, empresas, equipe, financeiro…" else "Status, corridas e ganhos",
            icone = if (painel == Painel.MOTOBOY) PwIcones.ShieldCheck else PwIcones.Bike,
            onClick = { onTrocar(if (painel == Painel.MOTOBOY) Painel.GESTOR else Painel.MOTOBOY) },
        )
        PwLinha("Verificar atualização", subtitulo = "Versão instalada: build ${Atualizador.versaoInstalada}", icone = PwIcones.Download,
            onClick = { Atualizador.abrirJanela(); Atualizador.verificar(automatico = false) })
    }
    PwBotaoPerigo("Sair da conta", onClick = onSair, icone = PwIcones.LogOut, modifier = Modifier.padding(top = 2.dp))
}
