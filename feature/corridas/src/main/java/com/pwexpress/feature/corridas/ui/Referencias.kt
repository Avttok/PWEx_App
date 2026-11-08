package com.pwexpress.feature.corridas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.feature.corridas.data.Referencia

/** Pontos de referência perto do endereço (até 3), em linhas discretas. */
@Composable
fun ReferenciasParada(referencias: List<Referencia>, modifier: Modifier = Modifier) {
    if (referencias.isEmpty()) return
    val c = Pw.cores
    Column(modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        referencias.forEach { r ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.Landmark, null, tint = c.ouro, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                if (r.relacao != null) Text(r.relacao, color = c.texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = Archivo)
                else {
                    Text(r.nome, color = c.texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = Archivo)
                    val extra = listOfNotNull(r.tipo, r.distanciaM?.let { if (it < 1000) "$it m" else "${it / 1000} km" }).joinToString(" · ")
                    if (extra.isNotEmpty()) Text("  $extra", color = c.texto3, fontSize = 12.sp)
                }
            }
        }
    }
}

/** Rotas automáticas: de qual cliente/pedido é a parada e os itens (com selos de perigoso/frágil). */
@Composable
fun ItensParada(p: com.pwexpress.feature.corridas.data.Parada, modifier: Modifier = Modifier) {
    if (p.cliente == null && p.pedidoNumero == null && p.itens.isEmpty()) return
    val c = Pw.cores
    Column(modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (p.cliente != null || p.pedidoNumero != null) {
            Text(
                listOfNotNull(p.cliente, p.pedidoNumero?.let { "pedido #$it" }).joinToString(" · "),
                color = c.texto2, fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp,
            )
        }
        p.itens.forEach { i ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.Package2, null, tint = if (i.perigoso) c.vermelho else c.texto3, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text("${i.quantidade}× ${i.descricao}", color = c.texto, fontSize = 12.5.sp, modifier = Modifier.weight(1f, fill = false))
                if (i.perigoso) { Spacer(Modifier.width(6.dp)); com.pwexpress.core.designsystem.PwSelo("PERIGOSO", c.vermelho, icone = PwIcones.Alerta) }
                if (i.fragil) { Spacer(Modifier.width(6.dp)); com.pwexpress.core.designsystem.PwSelo("FRÁGIL", c.ouro) }
            }
        }
    }
}
