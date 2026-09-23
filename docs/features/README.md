# Features

One document per feature of Magna, written for an agent that has to change one of them without
re-exploring the codebase first. Each one covers what the feature is for, the files that make it
up, how its data flows, the rules that are easy to break, and the tests that guard them.

These describe the code as of 2.0.3. Code is the source of truth. If a document and the code
disagree, trust the code and fix the document in the same change.

How the app is wired as a whole (modules, data pipeline, navigation, ViewModels, DI, release
pipeline) is in [../architecture.md](../architecture.md). Read it first.

## Paths

Paths are relative to `composeApp/src/commonMain/kotlin/com/tick/magna/` unless they start with
`composeApp/`, `androidApp/` or another top-level directory. Tests live under the same package
in `composeApp/src/commonTest/kotlin/com/tick/magna/`.

## Documents

| Feature | Document | Screens |
|---|---|---|
| Home, legislature choice and first sync | [home-e-legislatura.md](home-e-legislatura.md) | `HomeArgs` |
| Deputados: search, recent, profile | [deputados.md](deputados.md) | `DeputadosSearchArgs`, `DeputadoDetailsArgs` |
| Partidos | [partidos.md](partidos.md) | `PartidosListArgs`, `PartidoDetailsArgs` |
| Permanent committees | [comissoes.md](comissoes.md) | `ComissoesListArgs`, `ComissaoPermanenteDetailArgs` |
| Proposições | [proposicoes.md](proposicoes.md) | `ProposicoesListArgs`, `ProposicaoDetailsArgs` |
| Votações and the vote index | [votacoes.md](votacoes.md) | `VotacaoDetailArgs` |
| Santinho (private ballot note) | [santinho.md](santinho.md) | `SantinhoArgs` |

## Things every feature shares

- **Everything is scoped to the selected legislature.** `User.legislaturaId` is the single
  source. Repositories read it through `userDao.getUser()` and `flatMapLatest` on it, so
  switching terms re-reads every list by itself. A row from another term is not in the cache,
  which is ordinary and not an error (see the deputado crash fixed in `6c844bd`).
- **Cache first, network second.** Old terms never change, so their cache never expires. See
  `isCacheFresh` in `data/repository/CachePolicy.kt`: a finished term is fresh forever, a stamp
  in the future counts as stale, and a missing stamp is different from an empty result.
- **The Câmara API is the only backend.** It has hard limits that shape almost every
  repository: `/votacoes` refuses windows wider than about three months, several endpoints
  refuse `idLegislatura`, and pagination follows `next` links. The repositories document the
  measurements behind each decision in their KDoc. Read those before "simplifying" a request.
- **State shape.** A single `data class` per screen with nested sealed interfaces
  (`Loading / Empty / Error / Content`). `Content` is never built with an empty list; that is
  `Empty`. Mapping rules live in top-level `internal fun ...StateFor(result)` functions, so they
  can be tested without the ViewModel.
- **`_state.update { }`, never `_state.value = _state.value.copy(...)`.** Several screens run
  more than one coroutine on `Dispatchers.IO`, and read-modify-write lost updates have happened
  before. When a `combine` produces a state, it copies only the fields it owns and leaves the
  rest alone.
- **Areas.** Each part of the Câmara has an identity (`ui/core/theme/MagnaArea.kt`: DEPUTADOS,
  PARTIDOS, PROPOSICOES, COMISSOES, VOTACOES, LEGISLATURA) with its own colour and icon. A screen
  passes its area to `MagnaScreen`. A badge that links to another area uses that area's colour.
- **Analytics.** Screens report opens and empties through `AnalyticsInterface`, with an empty
  outcome tracked once per ViewModel. The santinho is the one exception, and it reports nothing
  at all.
