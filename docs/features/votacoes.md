# Votações and the vote index

How each deputado voted, and one screen per votação showing the result and every individual
vote. Both come from a local index that the app builds itself, because the API cannot answer
"how did this person vote".

## Files

- Deputado's votes tab: `features/deputados/details/DeputadoDetailsViewModel.kt` (a separate
  coroutine calling `getVotosDoDeputado`), `VotosState` in `DeputadoDetailsState.kt`
- Votação screen: `features/votacoes/detail/VotacaoDetailViewModel.kt`, `VotacaoDetailState.kt`,
  `VotacaoDetailScreen.kt`
- Data: `data/repository/votos/VotosRepository.kt`, `VotacaoNominal.kt`; `data/source/local/dao/VotoDao.kt`;
  tables `Voto.sq`, `VotoSync.sq`, `VotoImport.sq`

## How the index is built

- `/deputados/{id}/votos` returns 405, so the index is built from the other side, one votação
  at a time.
- The **first** profile opened in a term pays for a sweep of the window. Every other profile in
  that term then reads from the index for free. One quarter measured on 2026-09-19: 17 listing
  requests plus 17 nominal votações, about 6000 rows covering all 566 deputados.
- **Every órgão, not only the plenary.** A deputado does most of their work in committees.
  Plenary only would make the tab an attendance sheet.
- **Only nominal votações have individual votes**, and they are about 2% of the total (152 of
  7360 in 2026). `isVotacaoNominal` guesses from the description text (`Sim:` / `Resultado:`):
  it catches 147 of 152, with 2 false positives. The fields that would answer this exactly only
  exist in the annual file, not in the API.
- Votações already stored are skipped. The whole sweep is fresh for 6 h (`MAX_AGE`), and forever
  on a finished term. If the sweep fails, the stale index is used. It is only an error when
  nothing was ever indexed.

## Votação screen

- It reads **only** the index, with no network, and it works offline. You only reach it from a
  vote card, and a vote card exists only if its window was already swept.
- There are three outcomes plus Loading: `Content`, `Error`, and `NotFound`. `NotFound` is not
  an error: it happens on a restored deep link or after switching terms (`votacaoDetailStateFor`).
- The screen has its own area colour (VOTACOES). The proposição badge on it uses the PROPOSICOES
  colour, because it links there.

## Rules that are easy to break

- **Empty is the ordinary outcome of the votes tab.** It should say that the Câmara recorded no
  individual votes in this window, never suggest the deputado didn't vote.
- **The annual download was built and then removed** (`ddd1de8`). The files are rebuilt every
  night, so a "refresh" button tied to them would have asked for 20.7 MB every day. And
  downloading the whole Câmara from one person's profile reads as downloading that person. The
  `VotoImport` table and `VotoDao` support for it are still there. Don't bring the button back
  without solving both problems.
- Keep the votes coroutine outside the profile's `combine`. The first sweep of a term takes a
  while and must not hold up the header or the expenses.

## Tests

`data/repository/votos/VotacaoNominalTest.kt`, `features/deputados/details/TomDoVotoTest.kt`,
`features/comissoes/permanentes/detail/VotacoesStateTest.kt` (committee votações, a separate path
in `OrgaosRepository`)
