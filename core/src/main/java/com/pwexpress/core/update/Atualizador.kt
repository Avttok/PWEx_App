package com.pwexpress.core.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.edit
import com.pwexpress.core.PwCore
import com.pwexpress.core.config.PwConfig
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.long
import com.pwexpress.core.json.str
import com.pwexpress.core.notificacoes.PwNotificacoes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import okhttp3.Request
import java.io.File

/** Uma versão publicada no GitHub (Releases "build-N" com o APK anexado). */
data class Versao(val build: Int, val nome: String, val notas: String, val urlApk: String, val tamanho: Long, val publicadaEm: String?)

sealed interface EstadoAtualizacao {
    data object Ocioso : EstadoAtualizacao
    data object Verificando : EstadoAtualizacao
    data object EmDia : EstadoAtualizacao
    data class Disponivel(val versao: Versao) : EstadoAtualizacao
    data class Baixando(val versao: Versao, val baixado: Long, val total: Long) : EstadoAtualizacao {
        val fracao: Float get() = if (total > 0) (baixado.toFloat() / total).coerceIn(0f, 1f) else 0f
    }
    data class Instalando(val versao: Versao) : EstadoAtualizacao
    data class Erro(val mensagem: String, val versao: Versao?) : EstadoAtualizacao
}

/**
 * Atualização do app sem loja: procura no GitHub a versão mais nova, baixa mostrando o progresso
 * e entrega ao instalador do Android (o mesmo certificado garante que é uma atualização).
 */
object Atualizador {
    private var app: Context? = null
    private var buildAtual = 0
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var download: Job? = null

    private val _estado = MutableStateFlow<EstadoAtualizacao>(EstadoAtualizacao.Ocioso)
    val estado: StateFlow<EstadoAtualizacao> = _estado.asStateFlow()

    /** Janela aberta (o app mostra o diálogo quando true). */
    private val _janela = MutableStateFlow(false)
    val janela: StateFlow<Boolean> = _janela.asStateFlow()

    val versaoInstalada: Int get() = buildAtual

    fun init(context: Context, build: Int) {
        app = context.applicationContext
        buildAtual = build
        // apaga APKs de atualizações já instaladas
        runCatching { pasta(context).listFiles()?.forEach { it.delete() } }
    }

    private fun pasta(ctx: Context) = File(ctx.cacheDir, "atualizacao").apply { mkdirs() }
    private fun prefs() = app!!.getSharedPreferences("pwex_atualizacao", Context.MODE_PRIVATE)

    fun abrirJanela() { _janela.value = true }
    fun fecharJanela() {
        _janela.value = false
        val e = _estado.value
        if (e is EstadoAtualizacao.Disponivel) prefs().edit { putInt("adiada", e.versao.build) }
    }

    /** `automatico`: na abertura do app. Só abre a janela se for uma versão nova que ainda não foi adiada. */
    fun verificar(automatico: Boolean = false) {
        if (_estado.value is EstadoAtualizacao.Baixando || _estado.value is EstadoAtualizacao.Verificando) return
        escopo.launch {
            _estado.value = EstadoAtualizacao.Verificando
            val v = runCatching { buscarUltima() }.getOrElse {
                _estado.value = if (automatico) EstadoAtualizacao.Ocioso else EstadoAtualizacao.Erro("Não consegui consultar as atualizações. Verifique a internet.", null)
                return@launch
            }
            if (v == null || v.build <= buildAtual) {
                _estado.value = if (automatico) EstadoAtualizacao.Ocioso else EstadoAtualizacao.EmDia
                return@launch
            }
            _estado.value = EstadoAtualizacao.Disponivel(v)
            if (!automatico || prefs().getInt("adiada", 0) < v.build) _janela.value = true
        }
    }

    private suspend fun buscarUltima(): Versao? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("https://api.github.com/repos/${PwConfig.githubRepo}/releases?per_page=15")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "PWExpress-app")
            .build()
        PwCore.http.client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("github ${r.code}")
            Json.parseToJsonElement(r.body!!.string()).jsonArray
                .mapNotNull { it as? JsonObject }
                .mapNotNull { rel ->
                    val build = rel.str("tag_name")?.substringAfter("build-")?.toIntOrNull() ?: return@mapNotNull null
                    val apk = rel.arr("assets").firstOrNull { it.str("name")?.endsWith(".apk") == true } ?: return@mapNotNull null
                    Versao(
                        build = build,
                        nome = rel.str("name") ?: "build $build",
                        notas = rel.str("body").orEmpty().lines().filterNot { it.startsWith("Commit:") }.joinToString("\n").trim(),
                        urlApk = apk.str("browser_download_url") ?: return@mapNotNull null,
                        tamanho = apk.long("size") ?: 0L,
                        publicadaEm = rel.str("published_at"),
                    )
                }
                .maxByOrNull { it.build }
        }
    }

    fun baixar() {
        val v = when (val e = _estado.value) {
            is EstadoAtualizacao.Disponivel -> e.versao
            is EstadoAtualizacao.Erro -> e.versao ?: return
            else -> return
        }
        val ctx = app ?: return
        download?.cancel()
        download = escopo.launch {
            _estado.value = EstadoAtualizacao.Baixando(v, 0, v.tamanho)
            val arquivo = File(pasta(ctx), "PWExpress-build${v.build}.apk")
            val ok = runCatching {
                withContext(Dispatchers.IO) {
                    val req = Request.Builder().url(v.urlApk).header("User-Agent", "PWExpress-app").build()
                    PwCore.http.client.newBuilder().callTimeout(java.time.Duration.ofMinutes(10)).readTimeout(java.time.Duration.ofSeconds(60)).build()
                        .newCall(req).execute().use { r ->
                            if (!r.isSuccessful) error("download ${r.code}")
                            val corpo = r.body ?: error("vazio")
                            val total = corpo.contentLength().takeIf { it > 0 } ?: v.tamanho
                            var lido = 0L
                            var ultimo = 0L
                            corpo.byteStream().use { entrada ->
                                arquivo.outputStream().use { saida ->
                                    val buf = ByteArray(64 * 1024)
                                    while (isActive) {
                                        val n = entrada.read(buf)
                                        if (n < 0) break
                                        saida.write(buf, 0, n)
                                        lido += n
                                        if (lido - ultimo > 128 * 1024 || lido == total) {
                                            ultimo = lido
                                            _estado.value = EstadoAtualizacao.Baixando(v, lido, total)
                                            notificarProgresso(ctx, v, lido, total)
                                        }
                                    }
                                }
                            }
                            if (total > 0 && lido < total) error("incompleto")
                        }
                }
            }.isSuccess
            PwNotificacoes.cancelar(ctx, NOTIF)
            if (!ok) {
                arquivo.delete()
                _estado.value = EstadoAtualizacao.Erro("O download falhou. Tente de novo.", v)
                return@launch
            }
            instalar(ctx, arquivo, v)
        }
    }

    private const val NOTIF = 4001
    private var ultimoPct = -1

    /** Progresso na barra de notificações (para quando o app vai para o fundo durante o download). */
    private fun notificarProgresso(ctx: Context, v: Versao, lido: Long, total: Long) {
        val pct = if (total > 0) (lido * 100 / total).toInt() else 0
        if (pct == ultimoPct) return
        ultimoPct = pct
        val n = PwNotificacoes.base(ctx, PwNotificacoes.CANAL_ATUALIZACAO)
            .setContentTitle("Baixando a versão ${v.build}")
            .setContentText("${lido / 1_048_576} de ${total / 1_048_576} MB · $pct%")
            .setProgress(100, pct, total <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(PwNotificacoes.abrirApp(ctx, NOTIF))
            .build()
        runCatching { ctx.getSystemService(android.app.NotificationManager::class.java)?.notify(NOTIF, n) }
    }

    fun cancelarDownload() {
        app?.let { PwNotificacoes.cancelar(it, NOTIF) }
        download?.cancel()
        val e = _estado.value
        if (e is EstadoAtualizacao.Baixando) _estado.value = EstadoAtualizacao.Disponivel(e.versao)
    }

    /** O Android exige que o usuário libere "instalar apps desconhecidos" para este app. */
    fun podeInstalar(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ctx.packageManager.canRequestPackageInstalls()

    fun abrirPermissaoInstalar(ctx: Context) {
        val i = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(i) }
    }

    private suspend fun instalar(ctx: Context, arquivo: File, v: Versao) {
        _estado.value = EstadoAtualizacao.Instalando(v)
        val ok = runCatching {
            withContext(Dispatchers.IO) {
                val pi = ctx.packageManager.packageInstaller
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                    setAppPackageName(ctx.packageName)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
                val id = pi.createSession(params)
                pi.openSession(id).use { s ->
                    s.openWrite("pwexpress.apk", 0, arquivo.length()).use { out -> arquivo.inputStream().use { it.copyTo(out) }; s.fsync(out) }
                    val intent = Intent(ctx, InstaladorReceiver::class.java).setAction(InstaladorReceiver.ACAO)
                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
                    s.commit(PendingIntent.getBroadcast(ctx, id, intent, flags).intentSender)
                }
            }
        }
        if (ok.isFailure) _estado.value = EstadoAtualizacao.Erro("Não consegui abrir o instalador (${ok.exceptionOrNull()?.message}).", v)
    }

    /** Chamado pelo receiver com o resultado do instalador. */
    internal fun resultado(sucesso: Boolean, mensagem: String?) {
        val v = (_estado.value as? EstadoAtualizacao.Instalando)?.versao
        if (!sucesso) _estado.value = EstadoAtualizacao.Erro(mensagem ?: "A instalação foi cancelada.", v)
    }
}
