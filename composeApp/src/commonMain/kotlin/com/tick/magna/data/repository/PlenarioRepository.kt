package com.tick.magna.data.repository

import com.tick.magna.data.mapper.DeputadoMapper
import com.tick.magna.data.model.Deputado
import com.tick.magna.data.source.remote.api.DeputadosApiInterface

class PlenarioRepository(
    private val api: DeputadosApiInterface
): PlenarioRepositoryInterface {

    override suspend fun getDeputados(pagina: Int, itens: Int, ordem: String): Result<List<Deputado>> {
        return try {

            val response = api.getDeputados(
                pagina = pagina,
                itens = itens,
                ordem = ordem
            )
            val deputados = DeputadoMapper.fromDtoList(response.dados)
            Result.success(deputados)
        } catch (e: Exception) {
            Result.failure(Exception("Falha ao buscar deputados: ${e.message}"))
        }
    }

    override suspend fun getDeputadoById(id: Int): Result<Deputado> {
        return try {
            val response = api.getDeputadoById(id)
            val deputado = response.dados.firstOrNull()
            if (deputado != null) {
                Result.success(DeputadoMapper.fromDto(deputado))
            } else {
                Result.failure(Exception("Nenhum deputado encontrado com o ID $id"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erro ao buscar deputado: ${e.message}"))
        }
    }
}