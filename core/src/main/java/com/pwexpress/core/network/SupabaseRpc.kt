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

    /** Envia arquivo para um bucket do Storage. Ex.: enviarArquivo("comprovantes", "corrida/foto.jpg", bytes, "image/jpeg"). */
    suspend fun enviarArquivo(bucket: String, caminho: String, bytes: ByteArray, tipo: String): HttpResposta {
        val token = auth.tokenValido() ?: throw NaoAutenticadoException()
        val url = "/storage/v1/object/$bucket/$caminho"
        val r = http.postBytes(url, bytes, tipo, token)
        if (r.codigo != 401 && r.codigo != 403) return r
        val novo = auth.tokenValido(forcarRenovacao = true) ?: throw NaoAutenticadoException()
        return http.postBytes(url, bytes, tipo, novo)
    }
}
