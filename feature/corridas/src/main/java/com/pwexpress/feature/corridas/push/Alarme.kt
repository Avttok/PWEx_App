package com.pwexpress.feature.corridas.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.pwexpress.feature.corridas.R
import com.pwexpress.feature.corridas.ui.OfertaActivity

/** Notificações de corrida: a oferta (alta prioridade, som em loop, tela cheia) e avisos. */
object Alarme {
    // Mudou o som ou a vibração? Troque o id do canal: o Android não atualiza canais existentes.
    private const val CANAL_OFERTA = "pwex_ofertas_v1"
    private const val CANAL_AVISOS = "pwex_avisos"
    private const val CANAL_SILENCIOSO = "pwex_silencioso"
    private const val NOTIF_VOLTOU = 2003
    const val NOTIF_OFERTA = 2001
    private const val NOTIF_AVISO = 2002

    private fun criarCanais(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        val som = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${ctx.packageName}/${R.raw.pwex_oferta}")
        val audio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        nm.createNotificationChannel(
            NotificationChannel(CANAL_OFERTA, "Ofertas de corrida", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Toca alto e abre em tela cheia quando chega uma corrida"
                setSound(som, audio)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 900)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CANAL_AVISOS, "Avisos de corrida", NotificationManager.IMPORTANCE_HIGH)
        )
        nm.createNotificationChannel(
            NotificationChannel(CANAL_SILENCIOSO, "Avisos silenciosos", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Informativos que não tocam nem acendem a tela"
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    /** Aviso discreto: sem som, sem vibrar, sem acender a tela. */
    fun avisarSilencioso(ctx: Context, texto: String) {
        criarCanais(ctx)
        val abrir = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.let {
            PendingIntent.getActivity(ctx, 12, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val n = NotificationCompat.Builder(ctx, CANAL_SILENCIOSO)
            .setSmallIcon(R.drawable.ic_notif_corrida)
            .setColor(0xFFD4AF37.toInt())
            .setContentTitle("PW Express")
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .build()
        ctx.getSystemService(NotificationManager::class.java)?.notify(NOTIF_VOLTOU, n)
    }

    fun mostrarOferta(ctx: Context) {
        criarCanais(ctx)
        val abrir = Intent(ctx, OfertaActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pi = PendingIntent.getActivity(ctx, 10, abrir, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, CANAL_OFERTA)
            .setSmallIcon(R.drawable.ic_notif_corrida)
            .setColor(0xFFD4AF37.toInt())
            .setContentTitle("Nova corrida!")
            .setContentText("Toque para ver e aceitar. Você tem 30 segundos.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setOngoing(true)
            .setTimeoutAfter(32_000)
            .build()
        n.flags = n.flags or Notification.FLAG_INSISTENT // repete o som até responder
        ctx.getSystemService(NotificationManager::class.java)?.notify(NOTIF_OFERTA, n)
    }

    fun pararOferta(ctx: Context) {
        ctx.getSystemService(NotificationManager::class.java)?.cancel(NOTIF_OFERTA)
    }

    fun avisar(ctx: Context, titulo: String, texto: String) {
        criarCanais(ctx)
        val abrir = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.let {
            PendingIntent.getActivity(ctx, 11, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val n = NotificationCompat.Builder(ctx, CANAL_AVISOS)
            .setSmallIcon(R.drawable.ic_notif_corrida)
            .setColor(0xFFD4AF37.toInt())
            .setContentTitle(titulo)
            .setContentText(texto)
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .build()
        ctx.getSystemService(NotificationManager::class.java)?.notify(NOTIF_AVISO, n)
    }

    /** Toca o som de oferta uma vez, sem abrir nada. */
    fun testarSom(ctx: Context) {
        criarCanais(ctx)
        val n = NotificationCompat.Builder(ctx, CANAL_OFERTA)
            .setSmallIcon(R.drawable.ic_notif_corrida)
            .setColor(0xFFD4AF37.toInt())
            .setContentTitle("Teste de som")
            .setContentText("É assim que uma nova corrida vai tocar.")
            .setAutoCancel(true)
            .setTimeoutAfter(6_000)
            .build()
        ctx.getSystemService(NotificationManager::class.java)?.notify(NOTIF_AVISO + 1, n)
    }

    /** Android 14+: o usuário precisa permitir notificações em tela cheia. */
    fun podeTelaCheia(ctx: Context): Boolean =
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            ctx.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() ?: true
        } else true
}
