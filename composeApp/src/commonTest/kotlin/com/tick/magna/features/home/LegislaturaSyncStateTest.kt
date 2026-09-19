package com.tick.magna.features.home

import com.tick.magna.data.usecases.SyncStep
import com.tick.magna.data.usecases.SyncUserInformationState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The same sync states read two ways.
 *
 * On a cold start a failure means the app has nothing; right after a term switch it means one
 * term did not download while everything else still works. Telling them apart is what keeps
 * the switch from hiding the selector behind a modal the person cannot answer.
 */
class LegislaturaSyncStateTest {

    @Test
    fun a_cold_start_never_produces_a_switch_state() {
        val coldStart = listOf(
            SyncUserInformationState.Initial,
            SyncUserInformationState.Downloading,
            SyncUserInformationState.Done,
            SyncUserInformationState.Retry(setOf(SyncStep.ORGAOS)),
        )

        coldStart.forEach { state ->
            assertNull(legislaturaSyncStateFor(state, switching = false), "$state")
        }
    }

    @Test
    fun a_switch_in_flight_is_syncing() {
        assertEquals(
            LegislaturaSyncState.Syncing,
            legislaturaSyncStateFor(SyncUserInformationState.Downloading, switching = true),
        )
    }

    @Test
    fun a_switch_that_succeeded_stops_saying_anything() {
        assertNull(legislaturaSyncStateFor(SyncUserInformationState.Done, switching = true))
    }

    @Test
    fun a_switch_that_lost_one_section_names_it() {
        assertEquals(
            LegislaturaSyncState.Incomplete(setOf(SyncStep.ORGAOS)),
            legislaturaSyncStateFor(
                SyncUserInformationState.Retry(setOf(SyncStep.ORGAOS)),
                switching = true,
            ),
        )
    }

    @Test
    fun a_switch_with_no_network_at_all_carries_no_sections() {
        assertEquals(
            LegislaturaSyncState.Incomplete(emptySet()),
            legislaturaSyncStateFor(SyncUserInformationState.Retry(), switching = true),
        )
    }

    @Test
    fun everything_failing_reads_as_a_connection_problem_not_as_five_sections() {
        // With no network every step fails on its own, so the honest set is all five. Listing
        // them would be accurate and useless: nobody has a committee problem, they have no
        // network. The empty set is the same situation seen one step earlier.
        assertTrue(LegislaturaSyncState.Incomplete(SyncStep.entries.toSet()).isEverythingDown)
        assertTrue(LegislaturaSyncState.Incomplete(emptySet()).isEverythingDown)

        assertFalse(LegislaturaSyncState.Incomplete(setOf(SyncStep.ORGAOS)).isEverythingDown)
    }

    @Test
    fun the_whole_screen_is_only_taken_over_when_there_is_no_switch() {
        assertTrue(HomeState(syncState = SyncUserInformationState.Downloading).isBlockingSync)

        assertFalse(
            HomeState(
                syncState = SyncUserInformationState.Downloading,
                legislaturaSync = LegislaturaSyncState.Syncing,
            ).isBlockingSync
        )

        // Including when it failed: the way back to a term that works is on that surface.
        assertFalse(
            HomeState(
                syncState = SyncUserInformationState.Retry(),
                legislaturaSync = LegislaturaSyncState.Incomplete(emptySet()),
            ).isBlockingSync
        )
    }
}
