package com.pwexpress.core.config

import com.pwexpress.core.BuildConfig

/** Configuração do servidor, definida em gradle.properties (ou variáveis de ambiente no CI). */
object PwConfig {
    val supabaseUrl: String = BuildConfig.SUPABASE_URL.trimEnd('/')
    val supabaseKey: String = BuildConfig.SUPABASE_KEY

    val configurado: Boolean
        get() = supabaseUrl.startsWith("https://") && supabaseKey.isNotBlank()
}
