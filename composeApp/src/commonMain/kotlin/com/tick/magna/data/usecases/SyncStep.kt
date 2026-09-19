package com.tick.magna.data.usecases

import com.tick.magna.data.analytics.AnalyticsEvent

/**
 * One step of the sync, named so that a failure can be attributed to a section instead of to
 * the whole screen.
 *
 * The screen needs the distinction because both outcomes look identical from the database: a
 * step that failed and a term that genuinely has nothing of that kind both leave an empty list
 * behind. Only the step knows which one happened.
 */
enum class SyncStep {
    PARTIDOS,
    SIGLA_TIPOS,
    DEPUTADOS,
    ORGAOS,
    LEGISLATURAS,
}

/**
 * Kept as a mapping rather than reusing the analytics enum directly, so the screen branches on
 * a domain type and the analytics catalogue stays the only thing that decides what is reported.
 */
internal fun SyncStep.toAnalyticsStep(): AnalyticsEvent.SyncStep = when (this) {
    SyncStep.PARTIDOS -> AnalyticsEvent.SyncStep.PARTIDOS
    SyncStep.SIGLA_TIPOS -> AnalyticsEvent.SyncStep.SIGLA_TIPOS
    SyncStep.DEPUTADOS -> AnalyticsEvent.SyncStep.DEPUTADOS
    SyncStep.ORGAOS -> AnalyticsEvent.SyncStep.ORGAOS
    SyncStep.LEGISLATURAS -> AnalyticsEvent.SyncStep.LEGISLATURAS
}
