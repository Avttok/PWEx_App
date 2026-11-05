package com.pwexpress.core.auth

import android.content.Context
import androidx.core.content.edit

data class Sessao(
    val accessToken: String,
    val refreshToken: String,
    val expiraEm: Long,
    val userId: String,
    val email: String,
)

class SessaoStore(context: Context) {
    private val prefs = context.getSharedPreferences("pwex_sessao", Context.MODE_PRIVATE)

    fun ler(): Sessao? {
        val access = prefs.getString("access", null) ?: return null
        val refresh = prefs.getString("refresh", null) ?: return null
        return Sessao(
            accessToken = access,
            refreshToken = refresh,
            expiraEm = prefs.getLong("expira", 0L),
            userId = prefs.getString("user_id", "").orEmpty(),
            email = prefs.getString("email", "").orEmpty(),
        )
    }

    fun salvar(s: Sessao) = prefs.edit {
        putString("access", s.accessToken)
        putString("refresh", s.refreshToken)
        putLong("expira", s.expiraEm)
        putString("user_id", s.userId)
        putString("email", s.email)
    }

    fun limpar() = prefs.edit { clear() }
}
