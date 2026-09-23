# Partidos

The parties of the selected term: a carousel on the Home, a list the person can reorder, and a
party screen with its header, its members and charts about who they are.

## Files

- Home carousel: `features/partidos/component/PartidosComponentViewModel.kt`, `PartidosComponent.kt`.
  Shows the first 8 in repository order.
- List: `features/partidos/list/PartidosListViewModel.kt`, `PartidosListScreen.kt`, `Reordenacao.kt`
  (drag arithmetic: `alvoDoArrasto`, `mover`). The screen also draws the hemicycle
  (`ui/component/hemiciclo/`).
- Detail: `features/partidos/details/PartidoDetailsViewModel.kt`, `PartidoDetailsState.kt`, `PartidoDetailsScreen.kt`
- Data: `data/repository/PartidosRepository.kt`, `PartidoDao`, tables `Partido.sq` and `PartidoOrdem.sq`
- Colour: `ui/core/theme/` (`CorDoPartido`) and `data/color/` (dominant colour read from the logo)

## Data

- **Order.** `getPartidos()` returns the person's chosen order first, then parties by how many
  deputados they had in the term. The ViewModels do not sort. One used to sort by a column the
  sync never fills, and that did nothing at all.
- **Reordering.** `onOrdemChanged` only writes (`setOrdem`). The new order comes back through the
  query flow. The screen's local copy exists only for the length of one drag gesture.
- **Header.** `getPartidoDetail` reads the stored row first and fills in gaps if the sync has
  not reached them. `/partidos/{id}` answers 400 for older terms, so the row is the source of
  truth, and the list endpoint's data is enough to show a header.
- **Party colour.** Read off the logo once per term. 12 of the 27 logo URLs for the 57th return
  404, and those are big parties (PL, MDB, REPUBLICANOS, UNIAO). That is a party without a
  colour, not an error. The theme has a fallback.
- **Members.** `getPartidoMembros` is two phases on one flow. The roster arrives first, then the
  four biographical fields the charts need are filled in. `Content.isLoadingDetails` is true
  while the second phase runs. Members are fetched with parallel requests behind a semaphore. A
  member whose record fails is left out, not stored blank, so the next visit tries again.

## Rules that are easy to break

- **`selectedChart` belongs to the screen.** The `combine` builds a fresh state and copies
  `selectedChart` back from the current one. Don't let the combine's defaults overwrite it.
- **`CURRENT_YEAR = 2026` is hard-coded** in `PartidoDetailsViewModel` for the age groups. It
  will be wrong in 2027. Replace it with `today()` from `data/repository/Today.kt` when you touch
  this screen.
- Someone who left the party and came back counts as one member (`c25086d`). Keep that when you
  change how members are grouped.
- Marking a party as "mine" became reordering. The analytics event names in
  `data/analytics/AnalyticsEvent.kt` still mention favourites for history. The one used now is
  `PartidosReordered`.

## Tests

`features/partidos/list/ReordenacaoTest.kt`, `data/repository/PartidoMembrosTest.kt`,
`ui/core/theme/CorDoPartidoTest.kt`, `data/color/CorDominanteTest.kt`,
`ui/component/hemiciclo/HemicicloLayoutTest.kt`
