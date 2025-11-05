package com.tick.magna.data.source.remote.dto

import androidx.compose.ui.util.fastJoinToString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.tick.magna.DeputadoDetails as DeputadoDetailsEntity

@Serializable
data class DeputadoByIdDto(
    @SerialName("id")
    val id: Long,

    @SerialName("uri")
    val uri: String,

    @SerialName("nomeCivil")
    val nomeCivil: String,

    @SerialName("ultimoStatus")
    val ultimoStatus: UltimoStatusDto,

    @SerialName("cpf")
    val cpf: String,

    @SerialName("sexo")
    val sexo: String,

    @SerialName("urlWebsite")
    val urlWebsite: String? = null,

    @SerialName("redeSocial")
    val redeSocial: List<String> = emptyList(),

    @SerialName("dataNascimento")
    val dataNascimento: String,

    @SerialName("dataFalecimento")
    val dataFalecimento: String? = null,

    @SerialName("ufNascimento")
    val ufNascimento: String,

    @SerialName("municipioNascimento")
    val municipioNascimento: String,

    @SerialName("escolaridade")
    val escolaridade: String
)

fun DeputadoByIdDto.toLocal(legislaturaId: String): DeputadoDetailsEntity {
    return DeputadoDetailsEntity(
        deputadoId = id.toString(),
        legislaturaId = legislaturaId,
        gabineteBuilding = ultimoStatus.gabinete.predio,
        gabineteRoom = ultimoStatus.gabinete.sala,
        gabineteTelephone = ultimoStatus.gabinete.telefone,
        gabineteEmail = ultimoStatus.gabinete.email,
        urlWebsite = urlWebsite,
        socials = redeSocial.fastJoinToString(),
    )
}