package com.tick.magna.data.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ScreenNameTest {

    @Test
    fun strips_package_and_args_suffix_from_an_object_route() {
        assertEquals("Home", "com.tick.magna.features.home.HomeArgs".toScreenName())
        assertEquals("DeputadosSearch", "com.tick.magna.features.deputados.search.DeputadosSearchArgs".toScreenName())
        assertEquals("PartidosList", "com.tick.magna.features.partidos.list.PartidosListArgs".toScreenName())
    }

    @Test
    fun strips_argument_placeholders_from_a_parameterised_route() {
        val route = "com.tick.magna.features.deputados.details.DeputadoDetailsArgs/{deputadoId}"

        assertEquals("DeputadoDetails", route.toScreenName())
    }

    @Test
    fun strips_multiple_argument_placeholders() {
        val route = "com.tick.magna.features.deputados.votacoes.DeputadoVotacoesArgs/{deputadoId}/{deputadoName}"

        assertEquals("DeputadoVotacoes", route.toScreenName())
    }

    @Test
    fun strips_optional_query_arguments() {
        val route = "com.tick.magna.features.partidos.details.PartidoDetailsArgs?partidoId={partidoId}"

        assertEquals("PartidoDetails", route.toScreenName())
    }

    @Test
    fun never_leaks_a_concrete_argument_value() {
        // Compose Navigation hands over the route pattern, not the filled route. This pins
        // that the helper would strip a value anyway if that ever changed.
        val name = "com.tick.magna.features.deputados.details.DeputadoDetailsArgs/204528".toScreenName()

        assertEquals("DeputadoDetails", name)
        assertFalse(name.contains("204528"))
    }

    @Test
    fun keeps_the_name_when_there_is_no_package_or_suffix() {
        assertEquals("Home", "Home".toScreenName())
    }

    @Test
    fun keeps_a_name_that_is_only_the_args_suffix() {
        // removeSuffix would leave an empty string, which would be useless in a report.
        assertEquals("Args", "com.tick.magna.Args".toScreenName())
    }
}
