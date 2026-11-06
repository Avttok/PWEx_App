package com.pwexpress.core.notificacoes

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pwexpress.core.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Identidade das notificações do app: canais organizados por assunto, logo redonda,
 * dourado da marca (violeta no modo teste) e atalhos para abrir o app.
 */
object PwNotificacoes {
    const val GRUPO_CORRIDAS = "pwex_grupo_corridas"
    const val GRUPO_SISTEMA = "pwex_grupo_sistema"

    /** Corrida em andamento (notificação ao vivo, sem som). */
    const val CANAL_CORRIDA = "pwex_corrida_atual"
    /** Corridas planejadas (reserva, lembrete, mudanças). */
    const val CANAL_PLANEJADAS = "pwex_planejadas"
    /** Cancelamentos e avisos importantes. */
    const val CANAL_AVISOS = "pwex_avisos"
    /** Informativos que não tocam. */
    const val CANAL_SILENCIOSO = "pwex_silencioso"
    /** Download da atualização do app. */
    const val CANAL_ATUALIZACAO = "pwex_atualizacao"

    const val DOURADO = 0xFFD4AF37.toInt()
    const val VIOLETA = 0xFFA78BFA.toInt()

    /** O app avisa aqui quando o modo teste muda (cor das notificações). */
    @Volatile var modoTeste: Boolean = false
    val cor: Int get() = if (modoTeste) VIOLETA else DOURADO

    fun criarCanais(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannelGroup(NotificationChannelGroup(GRUPO_CORRIDAS, "Corridas"))
        nm.createNotificationChannelGroup(NotificationChannelGroup(GRUPO_SISTEMA, "Sistema"))
        fun canal(id: String, nome: String, imp: Int, grupo: String, desc: String, semSom: Boolean = false) =
            NotificationChannel(id, nome, imp).apply {
                description = desc
                this.group = grupo
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                if (semSom) { setSound(null, null); enableVibration(false) }
            }
        nm.createNotificationChannels(listOf(
            canal(CANAL_CORRIDA, "Corrida em andamento", NotificationManager.IMPORTANCE_DEFAULT, GRUPO_CORRIDAS,
                "Progresso da corrida atual (etapas, endereço e distância).", semSom = true),
            canal(CANAL_PLANEJADAS, "Corridas planejadas", NotificationManager.IMPORTANCE_DEFAULT, GRUPO_CORRIDAS,
                "Novas corridas com hora marcada, reservas e lembretes."),
            canal(CANAL_AVISOS, "Avisos de corrida", NotificationManager.IMPORTANCE_HIGH, GRUPO_CORRIDAS,
                "Cancelamentos e mudanças importantes."),
            canal(CANAL_SILENCIOSO, "Avisos silenciosos", NotificationManager.IMPORTANCE_LOW, GRUPO_CORRIDAS,
                "Informativos que não tocam nem acendem a tela.", semSom = true),
            canal(CANAL_ATUALIZACAO, "Atualização do app", NotificationManager.IMPORTANCE_LOW, GRUPO_SISTEMA,
                "Progresso do download de uma versão nova.", semSom = true),
        ))
    }

    private val logos = mutableMapOf<Boolean, Bitmap>()

    /** Logo (PW + entregador) num círculo escuro: ícone grande das notificações. Violeta no modo teste. */
    fun logoRedonda(ctx: Context): Bitmap? {
        val teste = modoTeste
        logos[teste]?.let { return it }
        val res = if (teste) R.drawable.pwex_marca_violeta else R.drawable.pwex_marca_ouro
        val fonte = runCatching { BitmapFactory.decodeResource(ctx.resources, res) }.getOrNull() ?: return null
        val lado = 192
        val saida = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
        val tela = Canvas(saida)
        tela.drawCircle(lado / 2f, lado / 2f, lado / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0B0B0D.toInt() })
        val larg = lado * 0.74f
        val alt = larg * fonte.height / fonte.width
        val destino = android.graphics.RectF((lado - larg) / 2f, (lado - alt) / 2f, (lado + larg) / 2f, (lado + alt) / 2f)
        tela.drawBitmap(fonte, null, destino, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        logos[teste] = saida
        return saida
    }

    fun iconeLogo(ctx: Context): Icon? = logoRedonda(ctx)?.let { Icon.createWithBitmap(it) }

    /** Abre o app (tela principal). `extra` vai junto no intent (ex.: abrir a aba de planejadas). */
    fun abrirApp(ctx: Context, codigo: Int, extra: String? = null): PendingIntent? =
        ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (extra != null) it.putExtra("pwex_abrir", extra)
            PendingIntent.getActivity(ctx, codigo, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

    /** Base com a identidade do app. */
    fun base(ctx: Context, canal: String): NotificationCompat.Builder {
        criarCanais(ctx)
        return NotificationCompat.Builder(ctx, canal)
            .setSmallIcon(R.drawable.ic_pwex_notif)
            .setLargeIcon(logoRedonda(ctx))
            .setColor(cor)
            .setSubText(if (modoTeste) "Modo teste" else null)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    /** Cartão expandido simples: título, texto curto e texto longo ao expandir. */
    fun aviso(
        ctx: Context, id: Int, canal: String, titulo: String, texto: String,
        longo: String? = null, abrir: PendingIntent? = abrirApp(ctx, id), acoes: List<NotificationCompat.Action> = emptyList(),
        silencioso: Boolean = false,
    ) {
        val b = base(ctx, canal)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(longo ?: texto))
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .setSilent(silencioso)
        acoes.forEach { b.addAction(it) }
        ctx.getSystemService(NotificationManager::class.java)?.notify(id, b.build())
    }

    fun cancelar(ctx: Context, id: Int) { ctx.getSystemService(NotificationManager::class.java)?.cancel(id) }

    val suportaAoVivo: Boolean get() = Build.VERSION.SDK_INT >= 36
}

/** Resumo da corrida atual para a notificação ao vivo (quem tem a corrida publica; o serviço de GPS mostra). */
data class ResumoCorrida(
    val numero: String,
    val titulo: String,          // "A caminho da coleta"
    val endereco: String,
    val etapaAtual: Int,         // 0-based: quantas etapas já foram concluídas
    val etapas: List<Boolean>,   // true = coleta, false = entrega (na ordem)
    val distanciaM: Int?,
    val valor: String?,
    val teste: Boolean,
)

object CorridaEmCurso {
    private val _resumo = MutableStateFlow<ResumoCorrida?>(null)
    val resumo: StateFlow<ResumoCorrida?> = _resumo.asStateFlow()
    fun publicar(r: ResumoCorrida?) { _resumo.value = r }
}
