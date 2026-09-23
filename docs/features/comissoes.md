# Permanent committees

The Câmara's 30 permanent committees, ordered by how busy they are in the selected term. A
committee's screen has three tabs: votações, composição (who sits on it) and presidentes.

## Files

- Home carousel: `features/comissoes/permanentes/component/ComissoesPermanentesViewModel.kt`,
  `ComissoesPermanentesComponent.kt`. Shows the top 10.
- Full list: `features/comissoes/permanentes/list/ComissoesListViewModel.kt`, `ComissoesListScreen.kt`
- Detail: `features/comissoes/permanentes/detail/ComissaoPermanenteDetailViewModel.kt`,
  `ComissaoPermanenteDetailState.kt`, `ComissaoPermanenteDetailScreen.kt`
- Data: `data/repository/orgaos/OrgaosRepository.kt`, `AtividadeWindow.kt`, `ComissaoCachePolicy.kt`,
  `ComissaoComposition.kt`; tables `Orgao`, `OrgaoAtividade`, `ComissaoCache`, `ComissaoMembro`, `ComissaoVotacao`

## Data

- **Which committees a term sees.** Committees are not stored per term. The same rows serve
  every term, filtered by each committee's `dataInicio`. Five of them only exist from 2023-02-15.
  A committee with no known start date is kept. `dataInicio` only exists on the detail endpoint
  (one request per committee), so it is only fetched when the user is not on the current term.
- **Ordering by activity.** Votes are counted in one March–June window per year of the mandate
  (`atividadeWindows`), which comes to about 120 requests instead of 450. Measured against the
  full count, it reproduces 8 of the top 10. The window avoids January and July because of the
  recess. This replaced six committee ids hard-coded in 2023.
- **Detail.** Votações and composição load in parallel. Presidentes only loads when its tab is
  opened: it covers the whole mandate, which is ten requests on the CCJC.
- **Cache per committee** (`isComissaoCacheFresh`): votações go stale after 6 h, composição and
  presidência after 7 days, and a finished term never refetches. A committee with no votes (the
  CASP) is stored as fetched-but-empty, so it is not downloaded again on every visit. The screen
  prefers stale data to an error, and old terms work offline.
- **Votes and composition ask about the same window:** the last three months of the mandate, or
  up to today if the mandate is still running. Without a window, `/votacoes` returns a recent
  slice of its own choosing, whatever term is selected.

## Rules that are easy to break

- **Four states per tab, five for presidentes** (`Idle` = never requested). Mapping lives in
  `votacoesStateFor`, `membrosStateFor` and `presidentesStateFor`. Before this, the screen
  treated an empty list as Loading and spun forever on committees that never vote.
- **A committee missing from the selected term** (it was created after the term ended) sets
  every tab to Error. Returning silently left the screen spinning.
- **Opening the presidentes tab again after an error retries** (`shouldLoadPresidentes`).
  Opening it before the committee resolved is handled in `init`.
- `hasComissoesPermanentes()` and `needsAtividade()` are what the Home's sync checks. See
  [home-e-legislatura.md](home-e-legislatura.md).

## Tests

`features/comissoes/permanentes/detail/VotacoesStateTest.kt`, `MembrosStateTest.kt`,
`data/repository/orgaos/AtividadeWindowTest.kt`, `ComissaoCachePolicyTest.kt`,
`ComissaoCacheFallbackTest.kt`, `ComissaoCompositionTest.kt`, `ComissaoDataInicioTest.kt`
