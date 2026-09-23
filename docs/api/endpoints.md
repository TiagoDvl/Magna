# Endpoints

Every call the app makes, as of 2.0.3. The API interfaces are in
`data/source/remote/api/*ApiInterface.kt`. One `HttpClient` serves all of them (see
[../architecture.md](../architecture.md#data-pipeline)).

## Deputados — `DeputadosApi`

| Call | Parameters | Caller | Stored in |
|---|---|---|---|
| `GET /deputados` | `idLegislatura`, `itens=1000`, `pagina` | `DeputadosRepository` (sync) | `Deputado`, key `(id, legislaturaId)` |
| `GET /deputados` (who is seated) | `dataInicio=dataFim=<reference date>`, `itens=1000`, `pagina` | `DeputadosRepository` (sync) | the `emExercicio` flag on `Deputado` |
| `GET /deputados/{id}` | — | `DeputadosRepository` (profile), `PartidosRepository` (member bios) | `DeputadoDetails` (per term), `DeputadoBio` (**not** per term) |
| `GET /deputados/{id}/despesas` | `idLegislatura`, `ano`, `ordem=DESC`, `ordenarPor=dataDocumento`, `itens=100` | `DeputadosRepository` | `DeputadoExpense` |

- `/deputados?idLegislatura` returns everyone who held a seat at any point in the term,
  substitutes included: 879 rows for 648 people in the 57th, against 513 seats. The dated call
  answers who was seated **on that day** (see [modelagem.md](modelagem.md#who-is-a-deputado-of-a-term)).
- Paging follows `next` links. The 57th fits in one page of 1000, the 55th (1138) does not.
- `GET /deputados/{id}/votos` and `/votacoes` return **405**. The relation from a deputado to
  their votes does not exist in this direction.

## Partidos — `PartidosApi`

| Call | Parameters | Caller | Stored in |
|---|---|---|---|
| `GET /partidos` | `idLegislatura`, `itens=100` | `PartidosRepository.syncPartidos` | `Partido`, key `(id, legislaturaId)` |
| `GET /partidos/{id}` | — | `PartidosRepository` (fills in the header once per term) | columns on `Partido` |
| `GET /partidos/{id}/membros` | `idLegislatura`, `itens=100`, `pagina` | `PartidosRepository.getPartidoMembros` | not stored (the bios go to `DeputadoBio`) |
| `GET <urlLogo>` (absolute URL) | `Accept: */*` | `PartidosRepository.corDoLogo` | the colour on `Partido` |

- `/partidos` has no size field and comes back in alphabetical order by sigla.
- `/partidos/{id}` only describes the **current** legislature. `?idLegislatura=56` returns 400.
  `status.totalMembros` is today's bench.
- The logo host answers **406** to `Accept: application/json`, which content negotiation adds to
  every request. `getLogo` overrides it. 12 of the 27 logo URLs of the 57th are 404.

## Proposições — `ProposicoesApi`

| Call | Parameters | Caller | Stored in |
|---|---|---|---|
| `GET /referencias/proposicoes/siglaTipo` | — | `ProposicoesRepository.syncSiglaTipos` | `SiglaTipo` |
| `GET /proposicoes` | `dataApresentacaoInicio`, `dataApresentacaoFim`, `ordem=desc`, `itens`, `pagina`, `siglaTipo` (repeated) | `ProposicoesRepository.refreshProposicoes` | `Proposicao`, per term |
| `GET /proposicoes` (count) | same window, `itens=1` | `contarNaJanela` | not stored (read from `last`) |
| `GET /proposicoes/{id}` | — | list refresh, detail screen | columns on `Proposicao` |
| `GET /proposicoes/{id}/autores` | — | list refresh, detail screen | the author columns on `Proposicao` |
| `GET /proposicoes/{id}/temas` | — | list refresh | `Proposicao.temas` |
| `GET /proposicoes/{id}/votacoes` | — | detail screen | not stored |
| `GET /proposicoes/{id}/tramitacoes` | — | detail screen | not stored |

- **Cost of one page of the list: 1 + 3 per proposição** (detail, authors, temas). It runs inside
  the collector's flow, so changing the filter cancels it. This is the most expensive call pattern
  in the app per screen.
- `siglaTipo` can be repeated to ask for several types in one request. TRAMITACAO has no list of
  siglas, so it takes the unfiltered page and filters with `NOT IN` in SQL.

## Órgãos (committees) — `OrgaosApi`

| Call | Parameters | Caller | Stored in |
|---|---|---|---|
| `GET /orgaos` | `codTipoOrgao=2`, `itens=100` | `OrgaosRepository.syncComissoesPermanentes` | `Orgao` (**not** per term) |
| `GET /orgaos/{id}` | — | `OrgaosRepository`, only off the current term | `Orgao.dataInicio` |
| `GET /orgaos/{id}/membros` | `dataInicio`, `dataFim`, `itens=100`, `pagina` | composition, presidents, seats of every deputado | `ComissaoMembro` |

- `codTipoOrgao=2` means permanent committee: 30 of them, one page.
- `/orgaos/{id}/eventos` works but only with a date window (without one it returns 0 items). The
  app does not use it yet.

## Votações — `VotacoesApi`

| Call | Parameters | Caller | Stored in |
|---|---|---|---|
| `GET /votacoes` (committee screen) | `idOrgao`, `dataInicio`, `dataFim`, `ordem=desc`, `ordenarPor=dataHoraRegistro`, `itens=20` | `OrgaosRepository` | `ComissaoVotacao` |
| `GET /votacoes` (activity count) | `idOrgao`, `dataInicio`, `dataFim`, `itens=1` | `OrgaosRepository` | `OrgaoAtividade`, per term |
| `GET /votacoes` (sweep) | `dataInicio`, `dataFim`, `itens=100`, `pagina` | `VotosRepository.sweep` | only the nominal ones |
| `GET /votacoes/{id}` | — | committee screen, up to 5 in parallel | `ComissaoVotacao` |
| `GET /votacoes/{id}/votos` | **no parameters** | `VotosRepository`, up to 5 in parallel | `Voto` |

- The committee screen walks back through the mandate in 3-month windows until it has 12
  votações, and stops after 4 windows at most (`ENOUGH_VOTACOES`, `MAX_WINDOWS_PER_SCREEN`).
- Activity ordering: one March–June window per year × 30 committees, about 120 requests, once per
  term.
- `/votacoes/{id}/votos` **refuses `itens` with 400** and returns every vote in one response
  (about 400 rows, `links` has only `self`). If you try to page it, you get an error or an empty
  list and conclude that there were no nominal votes. It is the most expensive trap on this API.

## Legislaturas — `LegislaturasApi`

| Call | Parameters | Caller | Stored in |
|---|---|---|---|
| `GET /legislaturas` | `itens=100`, `ordem=DESC`, `ordenarPor=id` | `LegislaturasRepository.syncLegislaturas` | `Legislatura` (`id`, `startDate`, `endDate`) |

All 57 terms fit in one page. **This is where every date window in the app comes from.** No date
should be hard-coded (the one that still is: `CURRENT_YEAR` in `PartidoDetailsViewModel`).

## Outside the API: annual files

`https://dadosabertos.camara.leg.br/arquivos/{resource}/{format}/{resource}-{year}.{format}`.
**The app does not use them** (the annual vote download was removed in `ddd1de8`), but they hold
data the API does not expose:

- `votacoesVotos-{ano}.csv`: vote → deputado. 2026 through September is 51,832 rows and
  16.4 MB. UTF-8 **with a BOM**, `;` separator, every field quoted, LF, **12 columns**
  (`deputado_uri` sits between `deputado_id` and `deputado_nome`). The same 466 empty `voto`
  values as the API (votação `2645346-18`).
- `votacoes-{ano}.csv`: carries `votosSim`, `votosNao` and `votosOutros`, which no endpoint
  returns.
- `deputados/csv/deputados.csv`: no year, 1.3 MB, 7889 rows, every deputado in history, with
  `siglaSexo`, `dataNascimento`, `ufNascimento` and `municipioNascimento`. The id comes from the
  end of `uri`. It could replace the per-member `/deputados/{id}` calls on the party screen.

They are **rebuilt every night** (so `Last-Modified` changes every day), are not compressed
(`gzip` returns the same bytes, and `HEAD` gives the real size), and are not in chronological
order, so a `Range` request cannot fetch only the new rows. Prefer CSV to JSON: it is half to a
third of the size.
