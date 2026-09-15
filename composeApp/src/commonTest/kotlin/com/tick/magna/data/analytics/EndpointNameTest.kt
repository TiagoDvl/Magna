package com.tick.magna.data.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class EndpointNameTest {

    @Test
    fun keeps_a_plain_collection_path() {
        assertEquals("deputados", "/api/v2/deputados".toEndpointName())
        assertEquals("partidos", "/api/v2/partidos".toEndpointName())
        assertEquals("orgaos", "/api/v2/orgaos".toEndpointName())
    }

    @Test
    fun masks_the_record_id_in_a_detail_path() {
        assertEquals("deputados/{id}", "/api/v2/deputados/204528".toEndpointName())
        assertEquals("partidos/{id}", "/api/v2/partidos/36779".toEndpointName())
    }

    @Test
    fun masks_the_id_in_the_middle_of_a_path() {
        assertEquals("deputados/{id}/despesas", "/api/v2/deputados/204528/despesas".toEndpointName())
        assertEquals("proposicoes/{id}/autores", "/api/v2/proposicoes/2543145/autores".toEndpointName())
    }

    @Test
    fun masks_ids_that_are_not_purely_numeric() {
        // Votação ids look like 2358471-1.
        assertEquals("votacoes/{id}/votos", "/api/v2/votacoes/2358471-1/votos".toEndpointName())
    }

    @Test
    fun never_lets_a_record_id_through() {
        val paths = listOf(
            "/api/v2/deputados/204528/despesas",
            "/api/v2/proposicoes/2543145",
            "/api/v2/votacoes/2358471-1/votos",
            "/api/v2/partidos/36779/membros",
        )

        paths.forEach { path ->
            val endpoint = path.toEndpointName()
            assertFalse(endpoint.any { it.isDigit() }, "'$endpoint' still carries digits from '$path'")
        }
    }

    @Test
    fun keeps_multi_segment_reference_paths_intact() {
        assertEquals(
            "referencias/proposicoes/siglaTipo",
            "/api/v2/referencias/proposicoes/siglaTipo".toEndpointName(),
        )
    }

    @Test
    fun drops_the_query_string() {
        assertEquals("deputados", "/api/v2/deputados?idLegislatura=57&itens=100".toEndpointName())
    }

    @Test
    fun works_without_the_api_prefix_or_leading_slash() {
        assertEquals("deputados/{id}", "deputados/204528".toEndpointName())
    }

    @Test
    fun falls_back_to_unknown_for_an_empty_path() {
        assertEquals("unknown", "".toEndpointName())
        assertEquals("unknown", "/".toEndpointName())
        assertEquals("unknown", "/api/v2/".toEndpointName())
    }

    @Test
    fun produces_a_bounded_set_of_values_across_the_whole_api() {
        // The point of masking is that the dimension stays small enough to read. Every
        // call the app makes should collapse onto one of these shapes.
        val allCalls = listOf(
            "/api/v2/deputados",
            "/api/v2/deputados/204528",
            "/api/v2/deputados/220599",
            "/api/v2/deputados/204528/despesas",
            "/api/v2/deputados/220599/despesas",
            "/api/v2/partidos",
            "/api/v2/partidos/36779",
            "/api/v2/partidos/36779/membros",
        )

        assertEquals(
            setOf(
                "deputados",
                "deputados/{id}",
                "deputados/{id}/despesas",
                "partidos",
                "partidos/{id}",
                "partidos/{id}/membros",
            ),
            allCalls.map { it.toEndpointName() }.toSet(),
        )
    }
}
