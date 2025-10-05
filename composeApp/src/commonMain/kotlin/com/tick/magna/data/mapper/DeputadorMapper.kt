package com.tick.magna.data.mapper

import com.tick.magna.data.model.Deputado
import com.tick.magna.data.source.remote.dto.DeputadoDto

internal object DeputadoMapper {
    fun fromDto(dto: DeputadoDto): Deputado {
        return Deputado(
            id = dto.id,
            nome = dto.nome,
            partido = dto.siglaPartido,
            uf = dto.siglaUf,
            fotoUrl = dto.urlFoto,
            email = dto.email
        )
    }

    fun fromDtoList(dtos: List<DeputadoDto>): List<Deputado> {
        return dtos.map { fromDto(it) }
    }
}