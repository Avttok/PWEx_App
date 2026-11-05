package com.pwexpress.core.network

import com.pwexpress.core.auth.AuthRepository
import kotlinx.serialization.json.JsonObject

class NaoAutenticadoException : Exception("Sessão expirada. Entre novamente.")

/** Chama funções do banco (RPC) com o token do usuário, renovando a sessão se necessário. */
class SupabaseRpc(private val http: SupabaseHttp, private val auth: AuthRepository) {

    suspend fun chamar(funcao: String, args: JsonObject): HttpResposta {
        val token = auth.tokenValido() ?: throw NaoAutenticadoException()
        val r = http.post("/rest/v1/rpc/$funcao", args.toString(), token)
        if (r.codigo != 401) return r
        val novo = auth.tokenValido(forcarRenovacao = true) ?: throw NaoAutenticadoException()
        return http.post("/rest/v1/rpc/$funcao", args.toString(), novo)
    }
}
