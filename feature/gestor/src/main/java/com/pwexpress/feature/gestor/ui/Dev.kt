package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwFiltros
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwNumero
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwPilula
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TipoAviso
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.bool
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.int
import com.pwexpress.core.json.long
import com.pwexpress.core.json.obj
import com.pwexpress.core.json.str
import com.pwexpress.core.perfil.Perfis
import com.pwexpress.feature.gestor.data.Gestao
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

private val CONTAGENS = listOf(
    "contas" to "Contas", "motoboys_online" to "Online", "corridas_ativas" to "Ativas", "corridas_reais" to "Corridas reais",
    "corridas_teste" to "De teste", "pings_24h" to "Pings 24 h", "pings_total" to "Pings no banco", "corridas_apagadas" to "Apagadas",
    "contas_excluidas" to "Contas excluídas",
)
private val PAPEIS = listOf(Triple("gestor", "Gestores", PwIcones.ShieldCheck), Triple("motoboy", "Motoboys", PwIcones.Bike), Triple("cliente", "Clientes", PwIcones.UserRound))

/** Console do Dev (igual ao do site): módulos liga/desliga e quem usa, sistema, aparelhos e registro. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DevG(onVoltar: () -> Unit) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var aba by remember { mutableIntStateOf(0) }
    var modulos by remember { mutableStateOf<List<Gestao.ModuloDev>?>(null) }
    var painel by remember { mutableStateOf<JsonObject?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }

    suspend fun carregar() {
        Gestao.modulos().onSuccess { modulos = it }.onFailure { erro = it.message }
        Gestao.painelDev().onSuccess { painel = it }.onFailure { erro = it.message }
    }
    LaunchedEffect(Unit) { carregar() }

    fun salvar(m: Gestao.ModuloDev, ligado: Boolean, papeis: List<String>) {
        modulos = modulos?.map { if (it.chave == m.chave) it.copy(ligado = ligado, papeis = papeis) else it }
        escopo.launch {
            Gestao.definirModulo(m.chave, ligado, papeis)
                .onSuccess { PwToast.mostrar("${m.nome}: ${if (ligado) "ligado" else "desligado"}"); Perfis.carregar() }
                .onFailure { PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO); carregar() }
        }
    }

    PwPagina {
        Cabecalho("Console Dev", onVoltar, subtitulo = "O sistema inteiro, sem filtro")
        PwFiltros(listOf("Módulos" to 0, "Sistema" to 0, "Aparelhos" to 0, "Registro" to 0), aba, { aba = it })
        PwErro(erro)
        when (aba) {
            0 -> {
                val l = modulos ?: run { PwCarregando(); return@PwPagina }
                TextoDica("Desligado, some para todos (menos para você). “Quem usa” limita a cargos.")
                l.groupBy { it.grupo }.forEach { (g, ms) ->
                    PwRotulo(g)
                    ms.forEach { m ->
                        PwCartao {
                            PwInterruptor(m.nome, m.ligado, { salvar(m, it, m.papeis) }, descricao = m.descricao)
                            if (m.ligado) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    PwPilula("Todos", PwIcones.Users, { salvar(m, true, emptyList()) }, ligada = m.papeis.isEmpty())
                                    PAPEIS.forEach { (p, rot, ic) ->
                                        val ativo = p in m.papeis
                                        PwPilula(rot, ic, { salvar(m, true, if (ativo) m.papeis - p else m.papeis + p) }, ligada = ativo)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                val cont = painel?.obj("contagens") ?: run { PwCarregando(); return@PwPagina }
                CONTAGENS.chunked(2).forEach { par ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        par.forEach { (k, r) -> PwNumero(r, "%,d".format(BR, cont.long(k) ?: 0L), Modifier.weight(1f)) }
                        if (par.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            2 -> {
                val ap = painel?.arr("aparelhos") ?: run { PwCarregando(); return@PwPagina }
                if (ap.isEmpty()) PwVazio("Nenhum aparelho registrado ainda.", icone = PwIcones.Smartphone)
                ap.forEach { a ->
                    PwCartao {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(a.str("nome") ?: "—", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(a.str("versao_app") ?: "—", color = c.texto3, fontSize = 12.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                            Detalhe(PwIcones.Radio, rotuloMotoboy[a.str("status")] ?: "Offline")
                            Detalhe(PwIcones.Timer, haQuanto(com.pwexpress.feature.gestor.data.iso(a.str("posicao_em"))))
                            a.int("bateria")?.let { Detalhe(PwIcones.BatteryMedium, "$it%") }
                            a.dbl("precisao_m")?.let { Detalhe(PwIcones.Crosshair, "± ${it.toInt()} m") }
                        }
                        if (a.bool("modo_teste") || a.bool("modo_economia")) Row(Modifier.padding(top = 4.dp)) {
                            if (a.bool("modo_teste")) Detalhe(PwIcones.FlaskConical, "teste", c.violeta)
                            if (a.bool("modo_economia")) { Spacer(Modifier.width(12.dp)); Detalhe(PwIcones.BatteryLow, "economia") }
                        }
                    }
                }
            }
            else -> {
                val ex = painel?.arr("exclusoes") ?: run { PwCarregando(); return@PwPagina }
                if (ex.isEmpty()) PwVazio("Nada apagado até agora.", icone = PwIcones.History)
                ex.forEach { x ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("${if (x.str("tipo") == "conta") "Conta" else "Corrida"} ${x.str("rotulo") ?: ""}", color = c.texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TextoDica(listOfNotNull(x.str("por")?.let { "por $it" }, com.pwexpress.feature.gestor.data.iso(x.str("quando"))?.let { dataHora(it) }, x.str("motivo")?.let { "“$it”" }).joinToString(" · "))
                    }
                }
            }
        }
    }
}
