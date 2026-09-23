# Proposições

The newest propositions filed in the selected term: four on the Home, a paginated list filtered
by what kind of instrument each one is, and a detail screen with authors, votações and the
tramitação timeline.

## Files

- Home: `features/proposicoes/component/RecentProposicoesViewModel.kt`, `RecentProposicoesComponent.kt`
- List: `features/proposicoes/list/ProposicoesListViewModel.kt`, `ProposicoesListState.kt`,
  `Paginacao.kt` (`deveCarregarMais`), `ProposicoesListScreen.kt`
- Detail: `features/proposicoes/details/ProposicaoDetailsViewModel.kt`, `ProposicaoDetailsState.kt`,
  `ProposicaoAutores.kt`, `DiaDeTramitacao.kt`, `ProposicaoDetailsScreen.kt`
- Data: `data/repository/proposicoes/ProposicoesRepository.kt`, `ProposicaoWindow.kt`,
  `BucketContagens.kt`; `data/domain/ProposicaoBucket.kt`

## Data

- **The window.** `/proposicoes` refuses `idLegislatura` (400), and `dataInicio`/`dataFim` filter
  by tramitação, not by filing date. Asked for late 2018, they return propositions filed in 1991.
  The code uses `dataApresentacaoInicio`/`dataApresentacaoFim` over a window of about three months
  at the end of the term: today for the current term, the closing day for a finished one
  (`proposicaoWindow`).
- **Buckets.** Four of them: `CONSTITUICAO` (PEC), `LEI` (PL, PLP, MPV, PLV, PLN, PLC),
  `ATO_LEGISLATIVO` (PDL, PDC, PDS, PRC, PRN), and `TRAMITACAO`, which means everything else. It is
  defined by subtraction because there are 544 siglas and new ones appear. It uses `NOT IN` in
  SQL and has no count of its own from the API.
- **Counts on the chips.** Four requests, one record each: the window total plus the three
  closed buckets. TRAMITACAO is the total minus the others (`contagensPorBucket`). The numbers
  are very uneven (1 PEC against 8848 procedural items in one measured window), and showing them
  is why the list paginates at all.
- **Paging.** 20 per page. The Câmara is asked for page N, the rows go into the cache, and the
  list observes the cache with `LIMIT pagina * 20`, re-subscribing each time the limit grows.
  `temMais` only turns off when a response has no `next` link.
- **Cache per term.** `observeRecentProposicoes` re-reads when the term changes, so one term's
  rows never show up on another term's Home.

## Rules that are easy to break

- **`onCarregarMais` has to stay guarded** (`carregandoMais || !temMais || isLoading`). The scroll
  condition is true for many frames in a row, and without the guard each page was requested
  dozens of times. `deveCarregarMais` returns false on an empty list so page 2 never goes out
  before page 1.
- **A failed page keeps the rows already on screen** and leaves `temMais` alone, so scrolling
  again retries. A failed first page is only an error if the cache is empty too.
- **Changing the filter starts over** (`abrir`): back to page 1, the cache re-read, the first
  page fetched.
- **Detail costs no extra requests** beyond the detail call itself. The rapporteur's name and
  the authors' photos and parties come from the term's deputado roster. Authors that are
  committees still render, because the name is on the proposition row.
- The register writes `.` and `Indefinida` where it means "nothing". Hide those, don't print them.
- `agruparPorData` groups steps by day and does not re-sort. The repository already returns them
  newest first.

## Tests

`features/proposicoes/list/PaginacaoTest.kt`, `features/proposicoes/details/DiaDeTramitacaoTest.kt`,
`data/domain/ProposicaoBucketTest.kt`, `data/domain/AutoriaTest.kt`,
`data/repository/proposicoes/ProposicaoWindowTest.kt`, `BucketContagensTest.kt`
