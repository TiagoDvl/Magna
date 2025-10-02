package com.tick.magna.shared.data.repository

import com.tick.magna.shared.data.mapper.DeputadoMapper
import com.tick.magna.shared.data.model.Deputado
import com.tick.magna.shared.data.source.remote.api.DeputadosApi
import com.tick.magna.shared.data.result.Result

class DeputadosRepository(private val api: DeputadosApi) {

    suspend fun getDeputados(
        pagina: Int = 1,
        itens: Int = 20,
        ordem: String = "ASC"
    ): Result<List<Deputado>> {
        return try {

            val response = api.getDeputados(
                pagina = pagina,
                itens = itens,
                ordem = ordem
            )
            val deputados = DeputadoMapper.fromDtoList(response.dados)
            Result.Success(deputados)
        } catch (e: Exception) {
            Result.Error(e, "Erro ao buscar deputados: ${e.message}")
        }
    }

    suspend fun getDeputadoById(id: Int): Result<Deputado> {
        return try {
            val response = api.getDeputadoById(id)
            val deputado = response.dados.firstOrNull()
            if (deputado != null) {
                Result.Success(DeputadoMapper.fromDto(deputado))
            } else {
                Result.Error(
                    Exception("Deputado não encontrado"),
                    "Nenhum deputado encontrado com o ID $id"
                )
            }
        } catch (e: Exception) {
            Result.Error(e, "Erro ao buscar deputado: ${e.message}")
        }
    }
}