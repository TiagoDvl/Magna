# Domain objects and mapping to the UI

What the app's own types are, how data changes shape on its way from the API to the screen, and
where each kind of decision belongs. Architecture is in [architecture.md](architecture.md), the
API's own shapes in [api/](api/README.md).

Paths are relative to `composeApp/src/commonMain/kotlin/com/tick/magna/`.

## The four shapes

```
DTO (data/source/remote/dto)      what the API sent, tolerant, only the fields that are read
  │  toLocal(legislaturaId, …)
  ▼
Entity (SQLDelight, generated)    what is stored, keyed by term where values change by term
  │  toDomain()   (data/source/local/mapper)
  ▼
Domain (data/domain)              what the app reasons about, the only shape repositories return
  │  xStateFor(), computed properties, UI models
  ▼
Screen state / UI model           what one screen draws (features/<feature>/…State.kt)
```

- **Repositories only return domain objects** (or `Resource`/`Result` wrapping them). A DTO or an
  entity never reaches a ViewModel.
- **Name collisions are resolved with import aliases.** SQLDelight generates `com.tick.magna.Deputado`,
  and the domain has `com.tick.magna.data.domain.Deputado`. In mappers and repositories, write
  `import com.tick.magna.Deputado as DeputadoEntity`, and `... as DeputadoDomain` where both meet.
  Follow the same suffixes (`Entity`, `Domain`) for any new table.
- **DTO → domain directly** happens for data that is not stored: votação details on the way into
  the committee cache (`ProposicoesAfetadasDto.toDomain()`), members of a committee
  (`MembroOrgaoDto.toDomain()`), and the party roster.
- **Packed columns** are one of the few mapping rules that live on both sides, so change them
  together:
  - `Proposicao.autores` holds author ids joined by the repository's `AUTHOR_SEPARATOR`.
  - `Proposicao.temas` holds `TEMA_SEPARATOR` (`" | "`).
  - `DeputadoDetails.socials` holds the links joined with `", "` (`fastJoinToString` on the way
    in, `split(", ")` on the way out).

## Domain objects

All in `data/domain/`, plain `data class`es with no Android or Compose types.

| Object | What it is | Notes |
|---|---|---|
| `Legislatura` | A term, with ISO `startDate`/`endDate` | Where every date window comes from |
| `Deputado` | A person in a term's roster | `emExercicio: Boolean?`, where null means **not measured** |
| `DeputadoDetails` | A profile's office and links | `socials` is a map of network name to URL |
| `DeputadoExpense` | One reimbursement | `valorDocumento` is a raw `Double` so totals stay possible |
| `DeputadoMembro` | A party member plus the four bio fields the charts need | Comes from the roster plus `DeputadoBio` |
| `ComissaoDoDeputado` | One committee seat seen from the person | `List.principal()` picks the seat worth printing: the highest office, ties broken by sigla |
| `Partido` | A party in the list | Three different sizes: `totalMembros` (official, today), `deputados` (local count over the term), `bancada` (seated on the reference date). Plus `posicao` (the user's order) and `cor` (raw ARGB from the logo) |
| `PartidoDetail` | A party's header | `totalPosse` and `totalMembros` side by side, because the pair shows the bench changing |
| `Lider` | A bench leader | `id` is nullable (terms synced before it was kept). A party with no leader has `lider = null`, and that is a real answer |
| `Orgao` | A committee | `votacoes: Int?`, where null means **not counted yet**. The SQL returns -1 and the mapper turns it into null |
| `MembroComissao` | One seat on a committee for one period | `cargo` groups `codTitulo` into MESA/TITULAR/SUPLENTE. The dates stay, because "current" is decided against a reference date |
| `Proposicao` | A list row | `identificacao` gives `PL 1589/2026`, or just the sigla. `autoria` holds the name and type from the API, while `autores` only exists for a deputado author's photo and party |
| `Autoria` / `TipoAutor` | Who signed | `autoriaDe()`: an unknown type is read as ORGAO, because drawing an institution with a person's icon is worse than the reverse |
| `ProposicaoDetail` | The detail screen | `temSituacao` decides whether the "Situação" section exists at all. The placeholders `.` and `Indefinida` are already null here |
| `TramitacaoProposicao`, `VotacaoDaProposicao` | Steps and votações of a proposition | |
| `ProposicaoBucket` | The four type groups | TRAMITACAO has no siglas: it is the `NOT IN` of the others |
| `ProposicoesNaJanela` | How many proposições were filed in the window | Context for "4 of 11333" |
| `Votacao` / `ProposicaoAfetada` | A committee votação | `descricao` is boilerplate. `parecer` is the substance, and `rotulo` is `PL 4770/2023` or null |
| `VotacaoDetalhe`, `ProposicaoVotada`, `VotoRegistrado` | A nominal votação from the index | `sim`/`nao`/`outros` are computed through `tomDoVoto`, so the tally and the tags always agree |
| `VotoDeputado` | One vote a deputado cast, with context | `voto` is the Câmara's own word |
| `TomDoVoto` | SIM / NAO / OUTRO | The five spellings collapse to the three questions a reader asks. Accent- and case-insensitive |

**Mocks** (`deputadosMock`, `deputadoExpensesMock`, `membrosComissaoMock`, …) live next to their
type and are used by Compose previews. Keep them compiling when you change a constructor, and
never use them outside `@Preview`.

## Mapping rules

### Keep "unknown" apart from "no" and "empty"

The rule that has caused the most bugs when broken:

- `Boolean?` / `Int?` where null means **not measured**: `emExercicio`, `Orgao.votacoes`,
  `Partido.posicao`, `Partido.cor`. The UI draws nothing for null. It never draws a negative or a
  zero.
- Sentinels from SQL (-1) and from the API (`.`, `Indefinida`, empty strings) become `null` in
  the mapper, not in the screen.
- An empty list and a failed load are different states (`Empty` vs `Error`), decided in
  `xStateFor(result)`.

### Drop the record, not the list

A mapper that can fail on one row returns null for that row (`mapNotNull`), and never throws for
the whole list:

- `Deputado.toDomain()` returns null without a name.
- `toComissoesPermanentes()` drops a committee with no name instead of drawing a blank card.
- `autoriaDe()` returns null without a name.
- Dates that cannot be parsed are passed through as they arrived (`toDisplayDate`), never thrown.

### Half a label is worse than none

Composite labels are built only when every part exists: `ProposicaoAfetada.rotulo`,
`ProposicaoVotada.rotulo` and `Proposicao.identificacao` (which falls back to the sigla). The
gabinete becomes an address only with a room (`salaDoGabinete`), and a phone number is only
dialable if it can be completed (`telefoneDiscavel`).

### Raw in the domain, formatted on the screen

Domain objects keep values in a form that still sorts and adds up:

- timestamps as the API sent them (`Votacao.dataHoraRegistro`, `VotoDeputado.dataHoraRegistro`,
  `MembroComissao.dataInicio`);
- money as `Double` (`DeputadoExpense.valorDocumento`), formatted with `toBrlString()` in
  `util/CurrencyFormat.kt`;
- colour as raw ARGB (`Partido.cor`), turned into a readable colour by the theme
  (`ui/core/theme/CorDoPartido.kt`, `comContraste`). The record says what the party's colour
  **is**, and the theme decides what it looks like on this surface. A null falls back to the
  area's colour.

Formatting helpers are in `util/` (`toBrlString`, `normalizeForSearch`, `percentEncoded`,
`appDeRedeSocial`), plus `toDisplayDate()` (`dd/MM/yyyy`, accepts both `2025-06-15` and
`2025-06-15T00:00:00`).

**Two exceptions still format in the mapper:** `DeputadoExpense.dataDocumento` and
`Proposicao.dataApresentacao` are turned into `dd/MM/yyyy` in `toDomain()`, so they no longer
sort as dates. Also, `toDisplayDate()` lives in `data/source/local/mapper/DeputadoExpenseMapper.kt`
even though screens import it. For new fields, keep the raw value in the domain and format it on
the screen. If you touch either of those two, move the formatting out too, and move
`toDisplayDate()` to `util/`.

### Domain → screen state

- **The rule goes in a named function, not inline in a composable.** Mapping a result to a state
  goes in `internal fun xStateFor(result)` (`votacoesStateFor`, `membrosStateFor`,
  `votacaoDetailStateFor`, `legislaturaSyncStateFor`). Anything the screen shows or hides based
  on the data goes in a computed property (`ProposicaoDetail.temSituacao`,
  `VotacoesState.Content.aprovadas`, `MembrosState.Content.mesa`). Both kinds can be tested
  without Compose.
- **`Content` is never built with an empty list**: that is `Empty`.
- **UI models exist only when a screen needs a different shape**, and they live with the
  feature: `features/comissoes/permanentes/component/domain/ComissaoPermanente.kt`, built by
  `toComissoesPermanentes()` and shared by the Home carousel and the full list so the two cannot
  disagree. Most screens draw domain objects directly. Don't add a UI model just to rename
  fields.
- **Collapse on purpose, in one place.** `TomDoVoto` turns five vote spellings into three
  colours, `CargoComissao` turns seven `codTitulo`s into three blocks, and `Regiao` turns 27 UFs
  into five regions. Put the grouping in the domain or in the feature's pure functions, never in
  a `when` inside a composable.

### Language of names

The domain mixes English from the first version (`Deputado.name`, `profilePicture`,
`Proposicao.type`) with the Câmara's Portuguese (`ementa`, `siglaPartido`, `codTitulo`,
`aprovacao`). New fields use the **API's own Portuguese name** when there is one, so they can be
searched from the JSON to the screen. Don't rename existing fields just to make them consistent:
that touches mappers, SQL and previews at the same time for no gain.

## Tests

`data/domain/AutoriaTest.kt`, `data/domain/ProposicaoBucketTest.kt`,
`data/source/local/mapper/DeputadoDetailsMapperTest.kt`, `DeputadoExpenseMapperTest.kt`,
`features/deputados/details/TomDoVotoTest.kt`, `ui/core/theme/CorDoPartidoTest.kt`,
`util/CurrencyFormatTest.kt`, `util/StringUtilsTest.kt`, `util/RedeSocialTest.kt`, and the
`*StateTest.kt` files under `features/`.
