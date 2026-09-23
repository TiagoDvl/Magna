# Parameters

How the common parameters really behave. A 200 does not mean a parameter did what its name says.

## Scoping to a legislature

The single most important table. The answer is different for each endpoint.

| Endpoint | `idLegislatura` | `dataInicio` / `dataFim` | How the app scopes it |
|---|---|---|---|
| `/deputados` | yes | yes (who was seated in the window) | `idLegislatura`, plus a one-day window for "seated" |
| `/deputados/{id}/despesas` | **required** for `ano` to work | — | `idLegislatura` + `ano` |
| `/partidos` | yes | — | `idLegislatura` |
| `/partidos/{id}` | **400** | — | current term only |
| `/partidos/{id}/membros` | yes | — | `idLegislatura` |
| `/proposicoes` | **400** | accepted, but they filter **tramitação**, not filing | `dataApresentacaoInicio` / `dataApresentacaoFim` |
| `/orgaos` | **400** | accepted, but they return nothing together with `codTipoOrgao` | cannot be scoped. Filter by each committee's `dataInicio` |
| `/orgaos/{id}/membros` | **400** | yes, **no width limit** | a window over the mandate |
| `/votacoes` | **400** | yes, **3 months at most** | 3-month windows |

The windows come from the `Legislatura` table (`startDate`, `endDate`), which comes from
`/legislaturas`. The current term ends its windows at today. A finished term ends them at its
last day.

## Date parameters

- **`dataInicio` / `dataFim`, format `yyyy-MM-dd`, inclusive.** A one-day window
  (`dataInicio == dataFim`) is valid and useful: on `/deputados` it answers who was seated that
  day, and on `/orgaos/{id}/membros` it returns every tie that crosses that day, with the tie's
  full period.
- **3-month ceiling** on `/votacoes` and `/proposicoes`. The error text is literally
  `A diferença entre as datas não pode ser maior que 3 meses`. Three and four months have passed
  in tests, six and twelve have failed. A four-year term is 16 windows. "All votações of the
  term" and "all proposições of the term" do not exist as a single call.
- **No window does not mean everything.** `/votacoes?idOrgao=2003` with no dates returns a
  recent quarter (2026-07-01 to 2026-09-01), not the full history, and nothing says so. Without
  dates, `/orgaos/{id}/membros` returns **today's** composition. On a past term that shows the
  right people under the wrong legislature, silently. `/orgaos/{id}/eventos` with no dates
  returns zero.
- **`/proposicoes`: `dataInicio`/`dataFim` filter by movement (tramitação).** Asked for
  Nov 2018–Jan 2019 with `ordem=asc`, it returns propositions filed in 1991, 1998 and 1999. The
  filing-date parameters are `dataApresentacaoInicio`/`dataApresentacaoFim`.
- **The first day of a term is useless** for "who was seated": every term's start date returns an
  empty list, because mandates are recorded as starting after it. Use the last day
  (`dataDeReferencia`).
- **`ano` is two different things:**
  - on `/despesas`, it is silently ignored without `idLegislatura`: `ano=2026` alone returns 0
    items, and with `idLegislatura=57` it returns 100.
  - on `/proposicoes`, it is the year in the proposition's **number** (`PL 1234/2018`), not the
    filing year: `ano=2018` returns items filed in 2019.

## Paging

- **`itens`: the ceiling is 100** on most endpoints (`itens=200` returns 100 and a `next`).
  `/deputados` is the exception, with a default and a ceiling of 1000.
- **Defaults differ** (15 on most, 1000 on `/deputados`), so never decide "was that the last
  page" by comparing the size you got with the size you asked for. Follow `links[rel=next]`
  (`hasNextPage()`), and cap the number of pages to protect against a `next` that never ends
  (`MAX_MEMBER_PAGES`, `MAX_SWEEP_PAGES`, and so on).
- **Write a term whole or not at all.** Collect every page before writing, so a failure halfway
  never leaves a half-stored term that looks complete.
- **Count without downloading:** `itens=1` and read the page number in `links[rel=last]`.
  `last` is missing when everything fits in the first page.
- **Some endpoints refuse paging:** `/votacoes/{id}/votos` returns 400 on `itens`.
- **Ordering does not sort within a page.** On `/orgaos/{id}/membros` the president can be on
  page 2, so read every page.

## Ordering

- `ordem` is `asc`/`desc` (case-insensitive in practice: the app sends `DESC` and `desc`).
- `/proposicoes` refuses `ordenarPor=dataApresentacao` with 400. `ordem=desc` sorts by **id**,
  which follows filing date closely enough to get a recent slice, but it is not the same thing.
- `/votacoes` accepts `ordenarPor=dataHoraRegistro`. An earlier version ordered by
  `idProposicaoObjeto` and presented an arbitrary slice as "the most recent".
- `/deputados/{id}/despesas` accepts `ordenarPor=dataDocumento`.

## Filters that are not filters

- `/orgaos`: `codTipoOrgao` combined with `dataInicio`/`dataFim` returns 200 with **zero** items.
- `/orgaos/{id}/membros`: `codTitulo`, `titulo` and `codTituloOrgao` are all 400. Filter on the
  client side.
- Repeating a parameter works where the API supports lists: `siglaTipo=PL&siglaTipo=PEC`.

## Headers

- **Never send `Accept-Charset`.** The gateway answers 403 with an HTML page to any value,
  including an empty one. Ktor's `HttpPlainText` adds it on every request. `StripAcceptCharset`
  in `HttpClientFactory.kt` removes it in the Send phase, and removing it earlier does not work.
- **Non-JSON resources** (logos) need `Accept: */*`. With the default `application/json` the
  server answers 406.
- `gzip` makes no difference on the annual files.
