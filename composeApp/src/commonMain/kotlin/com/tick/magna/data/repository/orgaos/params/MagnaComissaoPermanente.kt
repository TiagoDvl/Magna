package com.tick.magna.data.repository.orgaos.params

// These values are coming from: https://dadosabertos.camara.leg.br/api/v2/orgaos?codTipoOrgao=2
// codTipoOrgao = 2 means that I only want "Comissões Permanentes"
enum class MagnaComissaoPermanente(val idOrgao: String) {
    AGRO("2001"),
    CONSTITUICAO_E_JUSTICA("2003"),
    COMUNICACAO("539385"),
    SEGURANCA_PUBLICA("5503"),
    SAUDE("2014");
}
