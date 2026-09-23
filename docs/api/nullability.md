# Nullability

The register is uneven. Fields that are documented, or that look required, come back as `null`,
as empty strings, as placeholder text, or not at all. Differences between legislatures make this
worse. This is what has actually been seen, and how the DTOs cope.

## How parsing is set up

`apiJson()` in `data/source/remote/ApiJson.kt`, shared by the app and the tests:

- `ignoreUnknownKeys = true`: declare only what you read. A field you don't declare can never
  break parsing.
- `coerceInputValues = true`: an explicit `null` on a **non-null field that has a default**
  becomes the default.
- `isLenient = true`.

**The rule for DTOs:** only the id is required. Everything else is either nullable with
`= null`, or non-null with a default (`= ""`, `= 0`, `= emptyList()`). A list is deserialised in
one go, so **one record with a missing value fails the whole list**. That has happened more than
once: a deputado's details screen that never loaded, a party request that failed for DC and PSC,
a votação skipped on every refresh.

`coerceInputValues` does **not** save you from:
- a non-null field **without** a default that arrives as `null`, or goes missing;
- a nullable field **without** `= null` whose key goes missing (the key is then still required).

## What has been seen as null or empty

| Where | Field | What happens | Measured |
|---|---|---|---|
| `/votacoes/{id}/votos` | `tipoVoto` | **null on all 466 votes** of votação `2645346-18`: a nominal votação whose individual record was never filled in | 2026 |
| `/orgaos/{id}/membros` | `siglaPartido` | null in 58 of 138 rows (CCJC, 56th), never in the 57th. The app fills it from the local `Deputado` table (`withPartidoFromLocal`) | 56th vs 57th |
| `/partidos/{id}` | `status.data`, `status.situacao` | `"data": null` for DC and PSC | 57th |
| `/partidos/{id}` | `status.lider` | an object **full of nulls** instead of no object, when the party has no leader (DC, PSC) | 57th |
| `/partidos/{id}` | `urlLogo` | present but **404** for 12 of 27 parties (PL, MDB, REPUBLICANOS, UNIAO…) | 57th |
| `/deputados/{id}` | `ultimoStatus`, schooling, office | missing for some deputados. Required fields made the whole profile fail | — |
| `/deputados/{id}` → `ultimoStatus.gabinete.predio` | | `4` or `3`, and **`x`** once in 20 | 57th |
| `/deputados/{id}` → `gabinete.telefone` | | 8 digits, no area code (`3215-5420`) in 20 of 20 | 57th |
| `/deputados/{id}/despesas` | several | records missing fields. Everything except the natural key is optional | — |
| `/proposicoes/{id}` | `statusProposicao.regime`, `apreciacao` | the literal `.` or `Indefinida` when there is nothing to say. Hide them, don't print them | — |
| `/proposicoes/{id}` | `ementaDetalhada` | present in 2 of 4 | — |
| `/proposicoes/{id}/temas` | whole list | **empty for recent propositions**: 0 of 30 filed within 20 days, 29 of 30 six months later | 2026 |
| `/proposicoes/{id}/autores` | `nome`, `tipo` | not null, but the author is often **not a deputado** (a committee, the Executive), so resolving `uri` against the deputado table finds nobody | — |
| `/votacoes/{id}` | `ultimaApresentacaoProposicao` | missing when no proposition sits behind the vote | — |
| `/votacoes/{id}` | `efeitosRegistrados` | empty on 6 of 6 votações across 3 committees. Nothing should rely on it | 57th |
| `/orgaos/{id}` | `dataFim` | no permanent committee has one | — |
| `/orgaos` (list) | `dataInicio` | **not carried at all**. It only exists on `/orgaos/{id}` | — |
| `/legislaturas` | `dataInicio`, `dataFim` | expected, but a record without them is dropped when mapped (`hasPeriod()`) | — |
| `/deputados` (list) | `siglaUf`, `urlFoto`, `siglaPartido` | can be missing. A deputado with no UF matches no region | — |
| `/deputados` in-exercise flag | (derived) | `emExercicio = null` when the dated call failed. null means **unknown**, never "no" | — |

## DTO fields that are still strict

These would fail the whole response if the API sent null or dropped the key. None has failed in
production so far. If one does, relax it in the way described above:

- `VotacaoDetailDto`: `descricao: String`, `aprovacao: Int`, `proposicoesAfetadas: List<…>` (no
  defaults), and `dataHoraRegistro: String?` / `idEvento: String?` (nullable, but with no
  `= null`, so the key is required).
- `MembroOrgaoDto`: `nome`, `titulo`, `codTitulo`, `dataInicio`.
- `OrgaoDto`: `sigla`, `nome`, `nomeResumido`.
- `PartidoDto` / `PartidoDetalheDto`: `sigla`, `nome`, `uri`, and `status` (nullable with no default).
- `SiglaTipoDto`: `cod`, `sigla`, `nome`, and `descricao: String?` with no default.
- `ProposicaoAutorDto`: `uri`, `ordemAssinatura`.
- `LinkDto`: `rel`, `href`.

## How to handle a null in the app

- **Unknown is not no.** A null flag or date is drawn as nothing, never as a negative
  (`emExercicio`, a committee's `dataInicio`: a committee with no known start is kept).
- **Absent is not empty.** A cache stamp that is null means "never fetched", and that is
  different from "fetched, and there was nothing" (`isCacheFresh`).
- **Drop the record, not the list.** A vote with no `tipoVoto` is discarded. A member whose bio
  request failed is left out and not stored, so the next visit tries again. A legislature without
  a period is dropped.
- **Half a label is worse than none.** `ProposicoesAfetadasDto.toDomain()` builds `PL 4770/2023`
  only when all three parts are present.
- **Fill it from local data when you can.** The party of a past committee member comes from the
  `Deputado` table, and the rapporteur's name from the term's roster. That costs no request.
