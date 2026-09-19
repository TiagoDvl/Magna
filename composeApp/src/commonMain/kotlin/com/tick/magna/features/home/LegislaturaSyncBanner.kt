package com.tick.magna.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tick.magna.data.usecases.SyncStep
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaTheme
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.home_legislatura_incomplete
import magna.composeapp.generated.resources.home_legislatura_incomplete_offline
import magna.composeapp.generated.resources.home_legislatura_incomplete_retry
import magna.composeapp.generated.resources.home_legislatura_incomplete_sections
import magna.composeapp.generated.resources.home_legislatura_syncing
import magna.composeapp.generated.resources.home_legislatura_syncing_detail
import magna.composeapp.generated.resources.sync_step_deputados
import magna.composeapp.generated.resources.sync_step_legislaturas
import magna.composeapp.generated.resources.sync_step_orgaos
import magna.composeapp.generated.resources.sync_step_partidos
import magna.composeapp.generated.resources.sync_step_sigla_tipos
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * The line that keeps a term switch from looking like an empty app.
 *
 * Switching is the only action here that needs the network to produce anything visible, so it
 * is the one that shows a bad connection first. It sits above the sections rather than over
 * them: the selector stays reachable, which is also the way back to a term that does work.
 */
@Composable
internal fun LegislaturaSyncBanner(
    state: LegislaturaSyncState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val dimensions = LocalDimensions.current

    val container = when (state) {
        LegislaturaSyncState.Syncing -> colorScheme.surfaceContainer
        is LegislaturaSyncState.Incomplete -> colorScheme.errorContainer
    }
    val content = when (state) {
        LegislaturaSyncState.Syncing -> colorScheme.onSurface
        is LegislaturaSyncState.Incomplete -> colorScheme.onErrorContainer
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(container)
            .padding(dimensions.grid16),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        when (state) {
            LegislaturaSyncState.Syncing -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimensions.grid16),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(PROGRESS_SIZE),
                        color = colorScheme.tertiary,
                        strokeWidth = PROGRESS_STROKE,
                    )
                    Text(
                        text = stringResource(Res.string.home_legislatura_syncing),
                        style = typography.titleSmall.copy(color = content),
                    )
                }
                Text(
                    text = stringResource(Res.string.home_legislatura_syncing_detail),
                    style = typography.bodySmall.copy(color = content),
                )
            }

            is LegislaturaSyncState.Incomplete -> {
                Text(
                    text = stringResource(Res.string.home_legislatura_incomplete),
                    style = typography.titleSmall.copy(color = content),
                )
                // Read off the enum rather than the set so the order is the same every time.
                val sections = SyncStep.entries
                    .filter { step -> step in state.failedSteps }
                    .map { step -> stringResource(step.label()) }

                Text(
                    // Naming the sections is the whole difference between "this failed" and
                    // "this term has none of it", which look the same from the database.
                    text = if (sections.isEmpty()) {
                        stringResource(Res.string.home_legislatura_incomplete_offline)
                    } else {
                        stringResource(
                            Res.string.home_legislatura_incomplete_sections,
                            sections.joinToString(),
                        )
                    },
                    style = typography.bodySmall.copy(color = content),
                )
                TextButton(onClick = onRetry) {
                    Text(text = stringResource(Res.string.home_legislatura_incomplete_retry))
                }
            }
        }
    }
}

private fun SyncStep.label() = when (this) {
    SyncStep.PARTIDOS -> Res.string.sync_step_partidos
    SyncStep.SIGLA_TIPOS -> Res.string.sync_step_sigla_tipos
    SyncStep.DEPUTADOS -> Res.string.sync_step_deputados
    SyncStep.ORGAOS -> Res.string.sync_step_orgaos
    SyncStep.LEGISLATURAS -> Res.string.sync_step_legislaturas
}

private val PROGRESS_SIZE = 20.dp
private val PROGRESS_STROKE = 2.dp

@Preview
@Composable
private fun LegislaturaSyncBannerSyncingPreview() {
    MagnaTheme {
        LegislaturaSyncBanner(state = LegislaturaSyncState.Syncing)
    }
}

@Preview
@Composable
private fun LegislaturaSyncBannerPartialPreview() {
    MagnaTheme {
        LegislaturaSyncBanner(
            state = LegislaturaSyncState.Incomplete(setOf(SyncStep.ORGAOS, SyncStep.PARTIDOS)),
        )
    }
}

@Preview
@Composable
private fun LegislaturaSyncBannerOfflinePreview() {
    MagnaTheme {
        LegislaturaSyncBanner(state = LegislaturaSyncState.Incomplete(emptySet()))
    }
}
