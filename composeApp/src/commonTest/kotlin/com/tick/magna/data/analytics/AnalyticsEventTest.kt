package com.tick.magna.data.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnalyticsEventTest {

    /** One instance of every event, so the rules below cover the whole catalogue. */
    private val allEvents: List<AnalyticsEvent> = listOf(
        AnalyticsEvent.ScreenView("Home"),
        AnalyticsEvent.SyncStarted,
        AnalyticsEvent.SyncFinished(success = true, durationMs = 1234L),
        AnalyticsEvent.SyncStepFailed(AnalyticsEvent.SyncStep.ORGAOS),
        AnalyticsEvent.ContentEmpty(AnalyticsEvent.EmptyContent.PARTIDO_MEMBROS),
        AnalyticsEvent.DeputadoOpened(AnalyticsEvent.Source.RECENT),
        AnalyticsEvent.SearchPerformed(queryLength = 5, resultCount = 12, activeFilters = 2),
        AnalyticsEvent.ExpenseOpened(hasDocument = true),
        AnalyticsEvent.ExternalLinkOpened(AnalyticsEvent.LinkKind.EXPENSE_DOCUMENT),
        AnalyticsEvent.ProposicaoFilterChanged(tipo = "PEC"),
        AnalyticsEvent.ProposicaoOpened,
        AnalyticsEvent.PartidoOpened(AnalyticsEvent.Source.LIST),
        AnalyticsEvent.PartidoChartSelected(chart = "GENDER"),
        AnalyticsEvent.ComissaoOpened(sigla = "CCJC"),
        AnalyticsEvent.ApiError(endpoint = "deputados", status = 503),
        AnalyticsEvent.LegislaturaChanged(from = "57", to = "56"),
    )

    private val firebaseNamePattern = Regex("^[a-z][a-z0-9_]*$")

    @Test
    fun every_event_name_is_accepted_by_firebase() {
        allEvents.forEach { event ->
            assertTrue(
                firebaseNamePattern.matches(event.name),
                "event name '${event.name}' must be snake_case and start with a letter",
            )
            assertTrue(event.name.length <= 40, "event name '${event.name}' exceeds 40 chars")
        }
    }

    @Test
    fun no_event_name_uses_a_reserved_prefix() {
        val reserved = listOf("firebase_", "google_", "ga_")
        allEvents.forEach { event ->
            reserved.forEach { prefix ->
                assertFalse(event.name.startsWith(prefix), "'${event.name}' uses reserved prefix '$prefix'")
            }
        }
    }

    @Test
    fun every_parameter_key_is_accepted_by_firebase() {
        allEvents.forEach { event ->
            event.params.keys.forEach { key ->
                assertTrue(firebaseNamePattern.matches(key), "param key '$key' on '${event.name}' is not snake_case")
                assertTrue(key.length <= 40, "param key '$key' exceeds 40 chars")
            }
        }
    }

    @Test
    fun every_parameter_value_has_a_type_the_android_tracker_can_send() {
        val supported = allEvents.all { event ->
            event.params.values.all { it is String || it is Int || it is Long || it is Double || it is Boolean }
        }
        assertTrue(supported, "params must be String, Int, Long, Double or Boolean")
    }

    @Test
    fun string_parameter_values_stay_under_the_firebase_limit() {
        allEvents.forEach { event ->
            event.params.values.filterIsInstance<String>().forEach { value ->
                assertTrue(value.length <= 100, "value '$value' on '${event.name}' exceeds 100 chars")
            }
        }
    }

    @Test
    fun event_names_are_unique_across_the_catalogue() {
        val names = allEvents.map { it.name }
        assertEquals(names.size, names.toSet().size, "duplicate event name in catalogue")
    }

    @Test
    fun search_reports_the_query_length_and_never_the_query() {
        val event = AnalyticsEvent.SearchPerformed(queryLength = 7, resultCount = 3, activeFilters = 1)

        assertEquals(7, event.params[AnalyticsEvent.PARAM_QUERY_LENGTH])
        assertFalse(event.params.values.any { it is String })
    }

    @Test
    fun api_error_omits_status_when_the_request_got_no_answer() {
        val timedOut = AnalyticsEvent.ApiError(endpoint = "votacoes", status = null)
        val refused = AnalyticsEvent.ApiError(endpoint = "votacoes", status = 500)

        assertFalse(timedOut.params.containsKey(AnalyticsEvent.PARAM_STATUS))
        assertEquals(500, refused.params[AnalyticsEvent.PARAM_STATUS])
    }

    @Test
    fun screen_view_uses_the_parameter_names_firebase_expects() {
        val event = AnalyticsEvent.ScreenView("DeputadoDetails")

        assertEquals("screen_view", event.name)
        assertEquals("DeputadoDetails", event.params[AnalyticsEvent.PARAM_SCREEN_NAME])
        assertEquals("DeputadoDetails", event.params[AnalyticsEvent.PARAM_SCREEN_CLASS])
    }

    @Test
    fun sync_finished_carries_outcome_and_duration() {
        val event = AnalyticsEvent.SyncFinished(success = false, durationMs = 8_000L)

        assertEquals(false, event.params[AnalyticsEvent.PARAM_SUCCESS])
        assertEquals(8_000L, event.params[AnalyticsEvent.PARAM_DURATION_MS])
    }

    @Test
    fun every_enum_value_in_the_catalogue_is_reachable_from_the_app() {
        // A dimension value nothing can ever emit is worse than no value: the report looks
        // complete while silently missing a path. Source.AUTORES was exactly that until the
        // autores list on the proposicao screen became clickable.
        val wiredSources = setOf(
            AnalyticsEvent.Source.HOME_SEARCH,
            AnalyticsEvent.Source.RECENT,
            AnalyticsEvent.Source.SEARCH,
            AnalyticsEvent.Source.MEMBROS,
            AnalyticsEvent.Source.AUTORES,
            AnalyticsEvent.Source.HOME_SECTION,
            AnalyticsEvent.Source.LIST,
        )

        assertEquals(wiredSources, AnalyticsEvent.Source.entries.toSet())
    }

    @Test
    fun every_link_kind_in_the_catalogue_has_something_that_opens_it() {
        // DEPUTADO_WEBSITE was listed here while no screen ever rendered urlWebsite, which
        // would have shown up as a link kind that never appears in the report.
        val wiredKinds = setOf(
            AnalyticsEvent.LinkKind.EXPENSE_DOCUMENT,
            AnalyticsEvent.LinkKind.PROPOSICAO_FULL_TEXT,
            AnalyticsEvent.LinkKind.DEPUTADO_SOCIAL,
            AnalyticsEvent.LinkKind.PARTIDO_WEBSITE,
        )

        assertEquals(wiredKinds, AnalyticsEvent.LinkKind.entries.toSet())
    }

    @Test
    fun enum_backed_values_are_snake_case() {
        val values = AnalyticsEvent.Source.entries.map { it.value } +
            AnalyticsEvent.SyncStep.entries.map { it.value } +
            AnalyticsEvent.EmptyContent.entries.map { it.value } +
            AnalyticsEvent.LinkKind.entries.map { it.value }

        values.forEach { value ->
            assertTrue(firebaseNamePattern.matches(value), "enum value '$value' is not snake_case")
        }
    }

    @Test
    fun legislatura_changed_carries_both_ends_of_the_move() {
        val event = AnalyticsEvent.LegislaturaChanged(from = "57", to = "56")

        assertEquals("legislatura_changed", event.name)
        assertEquals("57", event.params[AnalyticsEvent.PARAM_FROM])
        assertEquals("56", event.params[AnalyticsEvent.PARAM_TO])
    }

    @Test
    fun sync_step_failed_names_the_step() {
        val event = AnalyticsEvent.SyncStepFailed(AnalyticsEvent.SyncStep.DEPUTADOS)

        assertEquals("deputados", event.params[AnalyticsEvent.PARAM_STEP])
    }
}
