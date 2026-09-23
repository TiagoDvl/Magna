# Câmara API

Everything Magna knows about the Câmara dos Deputados open-data API
(`https://dadosabertos.camara.leg.br/api/v2`): which endpoints the app calls, how their
parameters actually behave, where the payloads are null, what is hard to model, and what the
Portuguese terms mean.

| Document | Read it when |
|---|---|
| [endpoints.md](endpoints.md) | You need to know which call gets what, who calls it, and what it costs |
| [parametros.md](parametros.md) | You are about to add or change a query parameter |
| [nullability.md](nullability.md) | You are writing or changing a DTO |
| [modelagem.md](modelagem.md) | You are modelling a new entity or relation on top of the API |
| [glossario.md](glossario.md) | A field, a sigla or a domain word is unclear |

## The rule before anything else

**This is a snapshot, not a contract.** Most measurements were taken on the **57th legislature,
on 2026-09-19**. The API behaves differently across legislatures (paging on `/deputados` only
breaks from the 55th back, and `siglaPartido` in committee membership is null in half the rows
of the 56th and in none of the 57th). It also changes over time, with no visible versioning.

Before you work on a domain, **run a few of its requests yourself, on the legislature the work is
about.** It takes three requests:

1. Send the parameter you suspect. A 400 names the parameter it refused in `instance`:
   `{"status":400,"instance":"idLegislatura","detail":"Parâmetro(s) inválido(s)."}`.
2. Read `links[rel=last]` with `itens=1`. The page number in it is the record count, and you
   download nothing. No endpoint returns a total. See `totalFromLastPage()` in
   `data/source/remote/response/Pagination.kt`.
3. Compare the same call with and without the parameter. Some parameters are **silently ignored**
   instead of refused. Those are the dangerous ones: a refusal shows up in the log, a missing
   filter shows up nowhere.

When a measurement here turns out wrong, **fix it here and note the legislature and date**. Don't
work around it in code.

## Sources

- The measurements behind most of this come from `documentation/api-map.md` (pt-BR, 2026-09-19),
  the original survey. It is kept as the log of how each number was obtained. Some of its tables
  describe code that has changed since (its "who calls" line numbers, the 31-request proposições
  section, `/legislaturas` marked unused). These documents describe the code as it is now.
- KDoc on the DTOs (`data/source/remote/dto/`) and repositories (`data/repository/`) records the
  measurement next to the code that depends on it.
- `composeApp/src/commonTest/.../data/source/remote/ApiParsingTest.kt` parses real payload
  shapes with the production `apiJson()`.

Paths are relative to `composeApp/src/commonMain/kotlin/com/tick/magna/` unless they start with a
top-level directory.
