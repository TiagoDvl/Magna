# Home, legislature choice and first sync

The entry screen. It runs the sync that fills the local database, lets the person pick which
legislature they are looking at, and hosts one preview section per area, each leading to that
area's full screen.

## Files

- `features/home/MagnaHomeScreen.kt`: the layout. Header, santinho shortcut, sync banner,
  santinho banner, then the section components: `RecentDeputadosComponent`,
  `RecentProposicoesComponent`, `ComissoesPermanentesComponent` and `PartidosComponent`.
- `features/home/HomeViewModel.kt`, `HomeState.kt`
- `features/home/LegislaturaSheet.kt`: the term picker, opened from a calendar icon in the top
  bar. It renders nothing until the list of terms has arrived.
- `features/home/LegislaturaSyncBanner.kt`: reports a term switch in progress or a partial failure.
- `features/home/LegislaturaPeriodo.kt`: `Legislatura.periodo()`, which shows years only. The
  sheet and the header share it.
- `data/usecases/SyncUserInformationUseCase.kt`, `SyncStep.kt`
- `data/repository/user/UserRepository.kt`, `data/repository/legislaturas/LegislaturasRepository.kt`

## Flow

1. `HomeViewModel.init` calls `trySync()` and observes `legislaturaId` and the list of terms.
2. `SyncUserInformationUseCase`:
   - **NotConfigured** (first run): writes the user row and runs the full sync.
   - **Configured**: runs the sync only if `isLocalDataMissing()`. Otherwise it emits `Done`
     straight away, so coming back to a term that was downloaded before needs no network.
   - The full sync runs five steps in parallel: PARTIDOS, SIGLA_TIPOS, DEPUTADOS, ORGAOS and
     LEGISLATURAS. The result is `Done`, or `Retry(failedSteps)`.
3. Picking a term (`HomeAction.SelectLegislatura`) writes `User.legislaturaId`, sets
   `switchingLegislatura = true` and syncs again. Every repository re-reads through
   `flatMapLatest` on the user row.

## Rules that are easy to break

- **Cold start and term switch are different screens for the same sync state.**
  `legislaturaSyncStateFor(syncState, switching)` holds the rule. On a cold start a failure
  blocks the whole screen (`isBlockingSync`). After a switch the same failure is a banner, and
  the term picker stays reachable. `switching` stays true across a retry and is cleared only on
  `Done`.
- **`Incomplete.isEverythingDown`** (no steps ran, or all of them failed) is shown as "no
  connection" instead of listing five section names.
- **`isLocalDataMissing()` is what brings upgraded installs up to date.** Legislaturas and
  committee activity are part of the check because people upgrading from older versions are
  `Configured` but never had those tables filled. Committees are checked with
  `hasComissoesPermanentes()` and not by listing them: the list is filtered by term dates, and
  an old term can legitimately see an empty list while the table is full.
- **CancellationException is rethrown**, never turned into `Retry`. Leaving the screen is not
  a failed sync.
- A step that later needs a term's date window would have to wait for LEGISLATURAS instead of
  running in parallel with it. The comment in `syncInitialDependencies` explains why.

## Tests

`features/home/LegislaturaSyncStateTest.kt`, `data/usecases/SyncUserInformationUseCaseTest.kt`,
`data/repository/legislaturas/LegislaturasRepositoryTest.kt`, `data/repository/user/UserRepositoryTest.kt`
