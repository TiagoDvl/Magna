package com.tick.magna.data.source.remote.dto

import androidx.compose.ui.util.fastJoinToString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.tick.magna.DeputadoDetails as DeputadoDetailsEntity

/**
 * A deputado's full record.
 *
 * Only the id is required. Everything else is optional because the register is uneven: a
 * deputado with no recorded schooling or no office details used to fail deserialization,
 * which meant the details screen never loaded for them at all.
 */
@Serializable
data class DeputadoByIdDto(
    @SerialName("id")
    val id: Long,
    @SerialName("ultimoStatus")
    val ultimoStatus: UltimoStatusDto? = null,
    @SerialName("sexo")
    val sexo: String? = null,
    @SerialName("urlWebsite")
    val urlWebsite: String? = null,
    @SerialName("redeSocial")
    val redeSocial: List<String> = emptyList(),
    @SerialName("dataNascimento")
    val dataNascimento: String? = null,
    @SerialName("ufNascimento")
    val ufNascimento: String? = null,
    @SerialName("municipioNascimento")
    val municipioNascimento: String? = null,
)

fun DeputadoByIdDto.toLocal(legislaturaId: String): DeputadoDetailsEntity {
    return DeputadoDetailsEntity(
        deputadoId = id.toString(),
        legislaturaId = legislaturaId,
        gabineteBuilding = ultimoStatus?.gabinete?.predio,
        gabineteRoom = ultimoStatus?.gabinete?.sala,
        gabineteTelephone = ultimoStatus?.gabinete?.telefone,
        gabineteEmail = ultimoStatus?.gabinete?.email,
        urlWebsite = urlWebsite,
        socials = redeSocial.fastJoinToString(),
    )
}
