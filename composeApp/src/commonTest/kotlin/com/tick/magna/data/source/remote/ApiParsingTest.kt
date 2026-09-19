package com.tick.magna.data.source.remote

import com.tick.magna.data.source.remote.dto.LegislaturaDto
import com.tick.magna.data.source.remote.dto.ProposicoesAfetadasDto
import com.tick.magna.data.source.remote.dto.toDomain
import com.tick.magna.data.source.remote.dto.hasPeriod
import com.tick.magna.data.source.remote.dto.toLocal
import com.tick.magna.data.source.remote.response.DeputadoByIdResponse
import com.tick.magna.data.source.remote.response.DespesasResponse
import com.tick.magna.data.source.remote.response.DeputadosResponse
import com.tick.magna.data.source.remote.response.LegislaturasResponse
import com.tick.magna.data.source.remote.response.MembrosOrgaoResponse
import com.tick.magna.data.source.remote.response.ProposicoesResponse
import com.tick.magna.data.source.remote.response.VotacaoDetailResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Parses payloads shaped like the ones the Camara API actually returns, using the same
 * parser the app uses.
 *
 * The cases here are the ones that used to take a whole screen down: a record missing a
 * field, an explicit null, and an unknown field the API added.
 */
class ApiParsingTest {

    private val json = apiJson()

    @Test
    fun a_committee_vote_carries_the_rapporteur_and_the_proposition_label() {
        // Both were arriving in every response and being dropped: the screen showed the
        // descricao, which says "Aprovado o Parecer." in every committee measured.
        val payload = """
            {
              "dados": {
                "id": "2392193-60",
                "dataHoraRegistro": "2026-08-12T15:04:00",
                "descricao": "Aprovado o Parecer.",
                "aprovacao": 1,
                "idEvento": "82336",
                "ultimaApresentacaoProposicao": {
                  "descricao": "Parecer do Relator, Dep. Delegado Fabio Costa (PP-AL), pela constitucionalidade.",
                  "uriProposicao": "https://dadosabertos.camara.leg.br/api/v2/proposicoes/2392193"
                },
                "objetosPossiveis": [],
                "efeitosRegistrados": [],
                "proposicoesAfetadas": [
                  {
                    "id": "2392193",
                    "siglaTipo": "PL",
                    "numero": 4770,
                    "ano": 2023,
                    "ementa": "Altera a Lei 9.503, de 1997."
                  }
                ]
              }
            }
        """.trimIndent()

        val detail = json.decodeFromString<VotacaoDetailResponse>(payload).dados

        assertEquals(
            "Parecer do Relator, Dep. Delegado Fabio Costa (PP-AL), pela constitucionalidade.",
            detail.ultimaApresentacaoProposicao?.descricao,
        )
        assertEquals("PL 4770/2023", detail.proposicoesAfetadas.single().toDomain().rotulo)
    }

    @Test
    fun a_vote_with_no_presentation_parses_rather_than_failing() {
        val payload = """
            {
              "dados": {
                "id": "2392193-61",
                "dataHoraRegistro": null,
                "descricao": "Aprovada a Redação Final.",
                "aprovacao": 1,
                "idEvento": null,
                "proposicoesAfetadas": []
              }
            }
        """.trimIndent()

        val detail = json.decodeFromString<VotacaoDetailResponse>(payload).dados

        assertNull(detail.ultimaApresentacaoProposicao)
        assertTrue(detail.proposicoesAfetadas.isEmpty())
    }

    @Test
    fun a_proposition_missing_any_of_the_three_label_fields_has_no_label() {
        // Half a label reads as a typo. The ementa still carries the meaning.
        val semAno = ProposicoesAfetadasDto(id = "1", siglaTipo = "PL", numero = 4770, ementa = "Altera.")
        val semTipo = ProposicoesAfetadasDto(id = "2", numero = 4770, ano = 2023, ementa = "Altera.")

        assertNull(semAno.toDomain().rotulo)
        assertNull(semTipo.toDomain().rotulo)
        assertEquals("Altera.", semAno.toDomain().ementa)
    }

    @Test
    fun deputados_list_survives_a_record_with_nulls() {
        val payload = """
            {
              "dados": [
                {
                  "id": "204528",
                  "nome": "Baleia Rossi",
                  "siglaPartido": "MDB",
                  "siglaUf": "SP",
                  "urlFoto": "https://camara.leg.br/204528.jpg",
                  "email": "dep.baleiarossi@camara.leg.br"
                },
                {
                  "id": "999999",
                  "nome": "Sem Foto",
                  "siglaPartido": null,
                  "siglaUf": null,
                  "urlFoto": null,
                  "email": null
                }
              ],
              "links": []
            }
        """.trimIndent()

        val response = json.decodeFromString<DeputadosResponse>(payload)

        assertEquals(2, response.dados.size)
        assertEquals("Baleia Rossi", response.dados[0].nome)
        assertNull(response.dados[1].siglaUf)
        assertNull(response.dados[1].urlFoto)
    }

    @Test
    fun deputados_list_survives_a_record_missing_fields_entirely() {
        val payload = """
            { "dados": [ { "id": "204528" } ], "links": [] }
        """.trimIndent()

        val response = json.decodeFromString<DeputadosResponse>(payload)

        assertEquals("204528", response.dados.single().id)
        assertEquals("", response.dados.single().nome)
    }

    @Test
    fun deputados_list_ignores_fields_the_api_added() {
        val payload = """
            {
              "dados": [
                { "id": "204528", "nome": "Baleia Rossi", "campoNovoDaApi": "surpresa" }
              ],
              "links": []
            }
        """.trimIndent()

        val response = json.decodeFromString<DeputadosResponse>(payload)

        assertEquals("204528", response.dados.single().id)
    }

    @Test
    fun deputado_detail_parses_without_schooling_or_birthplace() {
        // A register entry with these missing used to fail, so the details screen never
        // loaded for that deputado at all.
        val payload = """
            {
              "dados": {
                "id": 204528,
                "nomeCivil": "BALEIA ROSSI",
                "cpf": null,
                "sexo": "M",
                "escolaridade": null,
                "dataNascimento": "1975-03-11",
                "ufNascimento": null,
                "municipioNascimento": null,
                "urlWebsite": null,
                "redeSocial": [],
                "ultimoStatus": {
                  "id": 204528,
                  "nome": "Baleia Rossi",
                  "gabinete": { "predio": "4", "sala": "101", "telefone": "3215-5555", "email": null }
                }
              },
              "links": []
            }
        """.trimIndent()

        val response = json.decodeFromString<DeputadoByIdResponse>(payload)
        val entity = response.dados.toLocal(legislaturaId = "57")

        assertEquals("204528", entity.deputadoId)
        assertEquals("4", entity.gabineteBuilding)
        assertEquals("101", entity.gabineteRoom)
        assertNull(entity.gabineteEmail)
    }

    @Test
    fun deputado_detail_parses_without_an_office_block_at_all() {
        val payload = """
            { "dados": { "id": 204528, "redeSocial": [] }, "links": [] }
        """.trimIndent()

        val response = json.decodeFromString<DeputadoByIdResponse>(payload)
        val entity = response.dados.toLocal(legislaturaId = "57")

        assertNull(entity.gabineteBuilding)
        assertNull(entity.gabineteRoom)
        assertNull(entity.gabineteTelephone)
        assertNull(entity.gabineteEmail)
    }

    @Test
    fun expenses_list_survives_a_record_missing_a_document_date() {
        // This is the case that emptied the expenses screen: one bad record in the list.
        val payload = """
            {
              "dados": [
                {
                  "ano": 2025, "mes": 6, "tipoDespesa": "COMBUSTIVEIS",
                  "codDocumento": "7938876", "parcela": 0,
                  "dataDocumento": "2025-06-15T00:00:00", "numDocumento": "118216",
                  "valorDocumento": 350.75, "urlDocumento": "https://camara.leg.br/a.pdf",
                  "nomeFornecedor": "POSTO IPIRANGA", "cnpjCpfFornecedor": "12345678000190",
                  "valorLiquido": 350.75, "valorGlosa": 0.0, "codLote": 1, "numRessarcimento": "",
                  "tipoDocumento": "Nota Fiscal", "codTipoDocumento": 0
                },
                {
                  "ano": 2025, "mes": 5, "tipoDespesa": "TELEFONIA",
                  "codDocumento": "7938877", "parcela": 0,
                  "dataDocumento": null, "numDocumento": null,
                  "valorDocumento": 120.0, "urlDocumento": null,
                  "nomeFornecedor": null, "cnpjCpfFornecedor": null
                }
              ],
              "links": []
            }
        """.trimIndent()

        val response = json.decodeFromString<DespesasResponse>(payload)

        assertEquals(2, response.dados.size)
        assertEquals(350.75, response.dados[0].valorDocumento)
        assertNull(response.dados[1].dataDocumento)
        assertEquals("7938877", response.dados[1].codDocumento)
    }

    @Test
    fun expenses_record_without_an_amount_defaults_to_zero_rather_than_failing() {
        val payload = """
            { "dados": [ { "codDocumento": "1", "valorDocumento": null } ], "links": [] }
        """.trimIndent()

        val response = json.decodeFromString<DespesasResponse>(payload)

        assertEquals(0.0, response.dados.single().valorDocumento)
        assertEquals(0, response.dados.single().ano)
    }

    @Test
    fun proposicoes_list_survives_a_record_without_an_ementa() {
        val payload = """
            {
              "dados": [
                { "id": 2543145, "codTipo": 136, "ementa": null, "dataApresentacao": null }
              ],
              "links": []
            }
        """.trimIndent()

        val response = json.decodeFromString<ProposicoesResponse>(payload)

        assertEquals(2543145, response.dados.single().id)
        assertNull(response.dados.single().ementa)
    }

    @Test
    fun a_response_with_no_records_is_not_an_error() {
        val response = json.decodeFromString<DespesasResponse>("""{ "dados": [], "links": [] }""")

        assertTrue(response.dados.isEmpty())
    }

    @Test
    fun legislaturas_list_keeps_the_period_of_each_term() {
        val payload = """
            {
              "dados": [
                {
                  "id": 57,
                  "uri": "https://dadosabertos.camara.leg.br/api/v2/legislaturas/57",
                  "dataInicio": "2023-02-01",
                  "dataFim": "2027-01-31"
                },
                {
                  "id": 56,
                  "uri": "https://dadosabertos.camara.leg.br/api/v2/legislaturas/56",
                  "dataInicio": "2019-02-01",
                  "dataFim": "2023-01-31"
                }
              ],
              "links": []
            }
        """.trimIndent()

        val response = json.decodeFromString<LegislaturasResponse>(payload)

        assertEquals(2, response.dados.size)
        assertEquals("2023-02-01", response.dados[0].dataInicio)
        assertEquals("2027-01-31", response.dados[0].dataFim)
        assertTrue(response.dados.all { it.hasPeriod() })
    }

    @Test
    fun a_legislatura_without_a_period_parses_but_is_not_usable() {
        val payload = """
            {
              "dados": [
                { "id": 57, "dataInicio": "2023-02-01", "dataFim": "2027-01-31" },
                { "id": 1, "dataInicio": null, "dataFim": null }
              ],
              "links": []
            }
        """.trimIndent()

        val response = json.decodeFromString<LegislaturasResponse>(payload)

        // The list still parses, which is the point: one unusable record must not cost the
        // other 56. The repository is what drops it, because the table requires both dates.
        assertEquals(2, response.dados.size)
        assertTrue(response.dados.first { it.id == 57 }.hasPeriod())
        assertFalse(response.dados.first { it.id == 1 }.hasPeriod())
    }

    @Test
    fun a_legislatura_maps_to_the_row_the_table_expects() {
        val dto = LegislaturaDto(id = 57, dataInicio = "2023-02-01", dataFim = "2027-01-31")

        val entity = dto.toLocal()

        assertEquals("57", entity.id)
        assertEquals("2023-02-01", entity.startDate)
        assertEquals("2027-01-31", entity.endDate)
    }

    @Test
    fun a_committee_member_parses_with_the_id_the_api_sends_as_a_number() {
        val payload = """
            {
              "dados": [
                {
                  "id": 178860,
                  "uri": "https://dadosabertos.camara.leg.br/api/v2/deputados/178860",
                  "nome": "Paulo Azi",
                  "siglaPartido": "UNIÃO",
                  "siglaUf": "BA",
                  "idLegislatura": 57,
                  "urlFoto": "https://www.camara.leg.br/internet/deputado/bandep/178860.jpg",
                  "email": "dep.pauloazi@camara.leg.br",
                  "titulo": "Titular",
                  "codTitulo": 101,
                  "dataInicio": "2026-02-09",
                  "dataFim": null
                }
              ],
              "links": []
            }
        """.trimIndent()

        val response = json.decodeFromString<MembrosOrgaoResponse>(payload)
        val membro = response.dados.single().toDomain()

        assertEquals("178860", membro.deputadoId)
        assertEquals(101, membro.codTitulo)
        assertNull(membro.dataFim)
    }

    @Test
    fun a_committee_member_of_a_past_term_parses_without_a_party() {
        // 58 of the 138 rows the CCJC returns for the 56th legislature look like this, and
        // none of the ones for the current term do. The party is filled in from the local
        // roster afterwards; what matters here is that the record does not fail to parse.
        val payload = """
            {
              "dados": [
                {
                  "id": 204379,
                  "nome": "Darci de Matos",
                  "siglaPartido": null,
                  "siglaUf": null,
                  "idLegislatura": 56,
                  "urlFoto": "https://www.camara.leg.br/internet/deputado/bandep/204379.jpg",
                  "titulo": "1º Vice-Presidente",
                  "codTitulo": 2,
                  "dataInicio": "2022-04-27",
                  "dataFim": "2023-01-31"
                }
              ]
            }
        """.trimIndent()

        val membro = json.decodeFromString<MembrosOrgaoResponse>(payload).dados.single().toDomain()

        assertNull(membro.siglaPartido)
        assertNull(membro.siglaUf)
        assertEquals("2023-01-31", membro.dataFim)
        assertEquals("1º Vice-Presidente", membro.titulo)
    }
}
