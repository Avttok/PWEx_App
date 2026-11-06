package com.pwexpress.feature.tracking.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.pwexpress.feature.tracking.Intervalos
import com.pwexpress.feature.tracking.Modo
import com.pwexpress.feature.tracking.R
import com.pwexpress.feature.tracking.Tracking
import com.pwexpress.feature.tracking.data.PontoGps
import com.pwexpress.feature.tracking.ui.Permissoes
import java.util.UUID

/**
 * Serviço em primeiro plano (notificação fixa) que coleta o GPS.
 * Continua rodando com a tela apagada ou o app fechado.
 */
class TrackingService : Service() {

    private lateinit var fused: FusedLocationProviderClient
    private var configAtual: Triple<Modo, Boolean, Long>? = null
    private var ultimoEnviado: Location? = null
    private var ultimoEnviadoEm = 0L
    private var redeCallback: ConnectivityManager.NetworkCallback? = null

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { processar(it) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        criarCanal()
        registrarRede()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val modo = Tracking.prefs.modo
        val economia = Tracking.prefs.economia

        // Regra do Android: após startForegroundService, chamar startForeground em até 5 s.
        try {
            val tipo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else 0
            ServiceCompat.startForeground(this, NOTIF_ID, notificacao(modo, economia), tipo)
        } catch (e: Exception) {
            Tracking.reportarErro("Rastreamento bloqueado pelo Android: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }

        if (modo == Modo.OFFLINE || !Permissoes.ler(this).localizacao) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        aplicar(modo, economia)
        return START_STICKY // se o Android matar o serviço, ele é recriado
    }

    override fun onDestroy() {
        fused.removeLocationUpdates(callback)
        redeCallback?.let {
            runCatching { getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(it) }
        }
        super.onDestroy()
    }

    // ------------------------------------------------------------------ GPS

    @SuppressLint("MissingPermission") // verificado em onStartCommand
    private fun aplicar(modo: Modo, economia: Boolean) {
        val nova = Triple(modo, economia, Intervalos.corridaMs(economia))
        if (nova == configAtual) return
        configAtual = nova

        fused.removeLocationUpdates(callback)
        ultimoEnviado = null

        val req = if (modo == Modo.EM_CORRIDA) {
            val intervalo = Intervalos.corridaMs(economia)
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalo)
                .setMinUpdateIntervalMillis(intervalo)
                .setWaitForAccurateLocation(false)
                .build()
        } else {
            LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, Intervalos.DISPONIVEL_LEITURA_MS)
                .setMinUpdateIntervalMillis(Intervalos.DISPONIVEL_LEITURA_MS / 2)
                .build()
        }
        fused.requestLocationUpdates(req, callback, Looper.getMainLooper())
        getSystemService(NotificationManager::class.java)?.notify(NOTIF_ID, notificacao(modo, economia))
    }

    private fun processar(loc: Location) {
        val modo = Tracking.prefs.modo
        when (modo) {
            Modo.OFFLINE -> return
            Modo.DISPONIVEL -> {
                val ult = ultimoEnviado
                val deveEnviar = ult == null ||
                    loc.time - ultimoEnviadoEm >= Intervalos.disponivelEnvioMs ||
                    loc.distanceTo(ult) >= Intervalos.disponivelDistanciaM
                if (!deveEnviar) return
            }
            Modo.EM_CORRIDA -> {
                if (loc.hasAccuracy() && loc.accuracy > Intervalos.PRECISAO_MAXIMA_M) return
            }
        }
        ultimoEnviado = loc
        ultimoEnviadoEm = loc.time

        Tracking.registrarPonto(
            PontoGps(
                idLocal = UUID.randomUUID().toString(),
                sessaoId = Tracking.prefs.sessaoId,
                status = modo,
                lat = loc.latitude,
                lng = loc.longitude,
                precisao = if (loc.hasAccuracy()) loc.accuracy else null,
                velocidade = if (loc.hasSpeed()) loc.speed else null,
                direcao = if (loc.hasBearing()) loc.bearing else null,
                altitude = if (loc.hasAltitude()) loc.altitude else null,
                bateria = bateria(),
                registradoEm = loc.time,
            )
        )
    }

    private fun bateria(): Int? =
        getSystemService(BatteryManager::class.java)
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            ?.takeIf { it in 0..100 }

    // ------------------------------------------------------------------ rede

    /** Quando a internet volta, esvazia a fila na hora. */
    private fun registrarRede() {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Tracking.sincronizarAgora()
            }
        }
        runCatching { cm.registerDefaultNetworkCallback(cb) }.onSuccess { redeCallback = cb }
    }

    // ------------------------------------------------------------------ notificação

    private fun criarCanal() {
        val canal = NotificationChannel(CANAL, "Rastreamento", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Aviso fixo enquanto o PW Express envia sua localização"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(canal)
    }

    private fun notificacao(modo: Modo, economia: Boolean): Notification {
        val texto = when (modo) {
            Modo.EM_CORRIDA -> "Em corrida · posição a cada ${Intervalos.corridaMs(economia) / 1000} s" +
                if (economia) " (economia)" else ""
            Modo.DISPONIVEL -> "Disponível · aguardando corridas"
            Modo.OFFLINE -> "Encerrando…"
        }
        val abrir = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(
                this, 0, it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        return NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(R.drawable.ic_notif_pwex)
            .setColor(0xFFD4AF37.toInt())
            .setContentTitle("PW Express")
            .setContentText(texto)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(abrir)
            .build()
    }

    private companion object {
        const val CANAL = "pwex_rastreamento"
        const val NOTIF_ID = 1001
    }
}
