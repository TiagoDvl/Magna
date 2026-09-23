# Deputados

Three entry points to the same people: the recently opened list on the Home, a searchable and
filterable list, and a profile with expenses, contacts and votes.

## Files

- Recent: `features/deputados/recent/RecentDeputadosViewModel.kt`, `RecentDeputadosComponent.kt`.
  Shows up to 10 people, ordered by `DeputadoLastSeen`.
- Search: `features/deputados/search/DeputadosSearchViewModel.kt`, `DeputadosSearchState.kt`,
  `DeputadosFiltro.kt` (pure filtering), `Regiao.kt`, `OpcoesFiltroSheet.kt`, `DeputadosSearchScreen.kt`
- Profile: `features/deputados/details/DeputadoDetailsViewModel.kt`, `DeputadoDetailsState.kt`,
  `DeputadoDetailsScreen.kt`, `DeputadoExpenseSheet.kt`, `Gabinete.kt`
- Data: `data/repository/deputados/DeputadosRepository.kt`, `DataDeReferencia.kt`,
  `data/source/local/dao/DeputadoDao.kt`, `data/repository/votos/VotosRepository.kt` (the votes
  tab, see [votacoes.md](votacoes.md)), `data/repository/orgaos/OrgaosRepository.kt` (committee seats)

## Data

- **Roster.** `syncDeputados` follows the `next` links until the end and writes the whole term
  at once, or not at all. The current term has about 879 rows and the 55th has 1138. The old
  code took one page of 1000 and silently lost people.
- **"Em exercício".** `/deputados?idLegislatura=` returns everyone who held a seat at any point
  in the term, substitutes included, so it lists more people than there are seats. Who is
  actually seated is asked with a date: today for the current term, the last day for a finished
  one (`dataDeReferencia`). `emExercicio` is null when that could not be asked, and null means
  "unknown", never "no".
- **Committee seats.** `orgaosRepository.syncComissoesMembros()` makes 30 requests, one per
  committee, instead of 513 (one per deputado). The search runs it in the background every time
  it opens. The repository decides freshness: a finished term never refetches, a live one
  refetches once a week.
- **Profile.** `combine(getDeputado, getDeputadoDetails, getDeputadoExpenses)`. Details and
  expenses use `cachedRecord`/`cachedList`: they emit the cache, then refresh from the API.
  Expenses are for the current calendar year. Votes load in a separate coroutine so they never
  hold up the combine.

## Rules that are easy to break

- **`getDeputado` can emit null, and must emit something.** The deputado may not be in the
  cache: the term changed, the id came from another term, or the database was reset. The DAO
  uses `mapToOneOrNull`. The repository emits `flowOf(null)` when there is no user. A flow that
  never emits holds the whole `combine`, and the whole screen, in Loading. `mapToOne` here was a
  crash in production (`6c844bd`).
- **All filters live in `DeputadosSearchState`**, not in `remember`, so a rotation keeps them.
  Every change goes through a single `_state.update { ...recalcular() }`.
- **UF and partido match exactly.** With `contains`, "PSD" would also match "PSDB" and "PT"
  would match "PTB". The name query is a normalised substring (`normalizeForSearch`, so accents
  are ignored).
- **Each filter's options are counted under all the other filters** (`opcoesUf(filtros.copy(uf = null))`),
  so no option leads to an empty list. Regions keep the constitution's order, not size order.
- **The committee filter is hidden until the seats have downloaded.** An empty seat map matches
  nobody.
- **Gabinete contacts** (`Gabinete.kt`): the register sends phone numbers as 8 digits with no
  area code, so `+55 61` is added. `predio` is only used as an annex when it is numeric (one
  real value is `x`). The map query leaves out the room number on purpose.
- Changing `DeputadoDetailsState` in a `combine`: copy only `deputado`, `detailsState` and
  `expensesState`. Building a whole state there once reset the selected tab on every emission.

## Tests

`features/deputados/search/DeputadosFiltroTest.kt`, `features/deputados/details/GabineteTest.kt`,
`features/deputados/details/TomDoVotoTest.kt`, `data/repository/deputados/DataDeReferenciaTest.kt`,
`data/repository/deputados/DeputadosSyncTest.kt`, `data/source/local/mapper/DeputadoDetailsMapperTest.kt`,
`data/source/local/mapper/DeputadoExpenseMapperTest.kt`
