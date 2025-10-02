package com.tick.magna.shared.data.mapper

import com.tick.magna.shared.data.model.Deputado
import com.tick.magna.shared.data.source.remote.dto.DeputadoDto

object DeputadoMapper {
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