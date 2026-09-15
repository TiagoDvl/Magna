package com.tick.magna.data.analytics

/**
 * Catalogue of every event Magna reports.
 *
 * Rules, enforced by AnalyticsEventTest:
 * - names and parameter keys are snake_case, start with a letter and stay under 40 characters;
 * - no free text ever leaves the device. A search term is reported as its length, never its
 *   content, and identifiers that point at a person are not reported at all;
 * - string values stay under 100 characters, the Firebase limit.
 */
sealed class AnalyticsEvent(
    val name: String,
    val params: Map<String, Any> = emptyMap(),
) {

    /**
     * Sent from a single place in App.kt, driven by the navigation back stack.
     * [screen] is the route pattern, so it never carries a real id.
     */
    data class ScreenView(val screen: String) : AnalyticsEvent(
        name = "screen_view",
        params = mapOf(PARAM_SCREEN_NAME to screen, PARAM_SCREEN_CLASS to screen),
    )

    data object SyncStarted : AnalyticsEvent("sync_started")

    /** Measures the Camara API more than it measures the app. */
    data class SyncFinished(val success: Boolean, val durationMs: Long) : AnalyticsEvent(
        name = "sync_finished",
        params = mapOf(PARAM_SUCCESS to success, PARAM_DURATION_MS to durationMs),
    )

    /**
     * Which of the four parallel first-run syncs failed. SyncFinished only says that the
     * whole thing failed, which is not actionable for an app whose first run depends on a
     * government API that goes down.
     */
    data class SyncStepFailed(val step: SyncStep) : AnalyticsEvent(
        name = "sync_step_failed",
        params = mapOf(PARAM_STEP to step.value),
    )

    /** A screen finished loading and had nothing to show. */
    data class ContentEmpty(val content: EmptyContent) : AnalyticsEvent(
        name = "content_empty",
        params = mapOf(PARAM_CONTENT to content.value),
    )

    data class DeputadoOpened(val source: Source) : AnalyticsEvent(
        name = "deputado_opened",
        params = mapOf(PARAM_SOURCE to source.value),
    )

    /** [queryLength] and [resultCount] only. The query itself stays on the device. */
    data class SearchPerformed(
        val queryLength: Int,
        val resultCount: Int,
        val activeFilters: Int,
    ) : AnalyticsEvent(
        name = "search_performed",
        params = mapOf(
            PARAM_QUERY_LENGTH to queryLength,
            PARAM_RESULT_COUNT to resultCount,
            PARAM_ACTIVE_FILTERS to activeFilters,
        ),
    )

    data class ExpenseOpened(val hasDocument: Boolean) : AnalyticsEvent(
        name = "expense_opened",
        params = mapOf(PARAM_HAS_DOCUMENT to hasDocument),
    )

    data class ExternalLinkOpened(val kind: LinkKind) : AnalyticsEvent(
        name = "external_link_opened",
        params = mapOf(PARAM_KIND to kind.value),
    )

    data class ProposicaoFilterChanged(val tipo: String) : AnalyticsEvent(
        name = "proposicao_filter_changed",
        params = mapOf(PARAM_TIPO to tipo),
    )

    /** Only one entry point exists today, so reporting a source would be a constant. */
    data object ProposicaoOpened : AnalyticsEvent("proposicao_opened")

    data class PartidoOpened(val source: Source) : AnalyticsEvent(
        name = "partido_opened",
        params = mapOf(PARAM_SOURCE to source.value),
    )

    data class PartidoChartSelected(val chart: String) : AnalyticsEvent(
        name = "partido_chart_selected",
        params = mapOf(PARAM_CHART to chart),
    )

    data class ComissaoOpened(val sigla: String) : AnalyticsEvent(
        name = "comissao_opened",
        params = mapOf(PARAM_SIGLA to sigla),
    )

    /** [status] is null when the request never got an answer, such as a timeout. */
    data class ApiError(val endpoint: String, val status: Int?) : AnalyticsEvent(
        name = "api_error",
        params = buildMap {
            put(PARAM_ENDPOINT, endpoint)
            status?.let { put(PARAM_STATUS, it) }
        },
    )

    /**
     * Which part of the app led somewhere. This is the most valuable dimension we collect:
     * it says which Home section people actually use.
     */
    enum class Source(val value: String) {
        HOME_SEARCH("home_search"),
        RECENT("recent"),
        SEARCH("search"),
        AUTORES("autores"),
        MEMBROS("membros"),
        HOME_SECTION("home_section"),
        LIST("list"),
    }

    enum class SyncStep(val value: String) {
        PARTIDOS("partidos"),
        SIGLA_TIPOS("sigla_tipos"),
        DEPUTADOS("deputados"),
        ORGAOS("orgaos"),
    }

    enum class EmptyContent(val value: String) {
        PARTIDO_MEMBROS("partido_membros"),
        PROPOSICAO_AUTORES("proposicao_autores"),
        COMISSAO_VOTACOES("comissao_votacoes"),
    }

    enum class LinkKind(val value: String) {
        EXPENSE_DOCUMENT("expense_document"),
        PROPOSICAO_FULL_TEXT("proposicao_full_text"),
        DEPUTADO_SOCIAL("deputado_social"),
        DEPUTADO_WEBSITE("deputado_website"),
        PARTIDO_WEBSITE("partido_website"),
    }

    companion object {
        const val PARAM_SCREEN_NAME = "screen_name"
        const val PARAM_SCREEN_CLASS = "screen_class"
        const val PARAM_SUCCESS = "success"
        const val PARAM_DURATION_MS = "duration_ms"
        const val PARAM_SOURCE = "source"
        const val PARAM_QUERY_LENGTH = "query_length"
        const val PARAM_RESULT_COUNT = "result_count"
        const val PARAM_ACTIVE_FILTERS = "active_filters"
        const val PARAM_HAS_DOCUMENT = "has_document"
        const val PARAM_KIND = "kind"
        const val PARAM_TIPO = "tipo"
        const val PARAM_CHART = "chart"
        const val PARAM_SIGLA = "sigla"
        const val PARAM_STEP = "step"
        const val PARAM_CONTENT = "content"
        const val PARAM_ENDPOINT = "endpoint"
        const val PARAM_STATUS = "status"

        /** User property, not an event parameter. */
        const val USER_PROPERTY_LEGISLATURA = "legislatura_id"
    }
}
