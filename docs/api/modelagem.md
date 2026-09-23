# Hard parts to model

The relations in this API are hard to keep in your head. There are many ids, many ways to scope a
query, and relations that only exist in one direction. Parts of the app were once modelled more
elaborately than they needed to be because of that. These are the areas where the obvious model
is wrong.

## A legislature is a key, not a column

`Deputado`, `Partido` and `DeputadoDetails` once had a primary key on `id` alone, with
`legislaturaId` as an ordinary column. The upsert overwrote the column, so each re-elected
deputado was **one row** that belonged to whichever term had synced last. The 57th and the 56th
share 332 people, so switching terms moved them back and forth. On top of that, a half-filled
term looked exactly like a full one.

Fixed in `4.sqm`: the key is `(id, legislaturaId)`.

**The rule:** anything whose values change by term is keyed by term. Things that don't change,
like birth data (`DeputadoBio`) or the list of committees (`Orgao`), are not.

## Who is a deputado of a term

There are three different answers, and they have different sizes:

- `/deputados?idLegislatura=57` returns **everyone who held a seat at any point**: 879 rows,
  648 people, against 513 seats, because substitutes are included.
- `/deputados?dataInicio=D&dataFim=D` returns **who was seated on day D**: 512 or 513, with the
  seats distributed exactly as the constitution sets them. D is today for the current term, and
  the last day for a finished one.
- `/partidos/{id}/membros?idLegislatura` returns everyone who **passed through the party** in the
  term, with **one row per period**: someone who left and came back appears twice (the PL has
  145 rows for 143 people). Deduplicate by id before counting.

A party's "size" is three numbers: the official `status.totalMembros` (today, current term only),
the count in the local `Deputado` table (anyone who sat for the party during the term, which
works for every term and costs nothing), and the number of seats. The list uses the local count
and labels it "deputados". The header uses the official one and labels it "membros". Keep the
two labels apart.

## Committees are not per term

`/orgaos?codTipoOrgao=2` returns the same 30 committees whatever term you want. They cannot be
scoped by date (see [parametros.md](parametros.md#filters-that-are-not-filters)).

- **Which ones existed in a term:** each committee's `dataInicio` is compared with the term.
  `dataInicio` only exists on `/orgaos/{id}`, and five committees start on 2023-02-15.
- **Who sat on one:** `/orgaos/{id}/membros` returns **one record per tie** (deputado, title,
  period), not one per person. A window returns **every tie that crosses it**. The rule for "in
  the committee at date R" is `dataFim == null || dataFim >= R`, which matched the undated call
  130 out of 130 on the CCJC.
- **The same person can hold two titles at once:** a president is also listed as titular. Sort by
  `codTitulo` before removing duplicates, so the higher title is kept.
- **The composition is annual**, not per term. The current CCJC ties all start in 2026.
- **Presidents rotate**, and the record **has holes that cannot be filled**: the CSSF has nothing
  between 2024-03-06 and 2025-03-19, and the CFT has a single president in four years. List what
  exists and say once that the record may be incomplete. Joining periods up to look continuous
  would invent presidents. On the CASP, the "presidents" are the presidents of the Câmara.

## Votes only go one way

- There is no path from a deputado to their votes (405). The only way in is
  `/votacoes/{id}/votos`, one votação at a time.
- **Only about 2% of votações have individual votes** (152 of 7360 in 2026, on 44 days of the
  year). Asking every votação for its votes would be 7360 requests for 152 answers.
- The only exact signal (`votosSim`/`votosNao`) is in the annual file, not the API. The app
  guesses from the description (`isVotacaoNominal`: `Sim:` or `Resultado:`), which catches 147
  of 152 with 2 false positives. Committee votações word it differently
  (`Resultado:  17 votos "Sim"`). Without `Resultado:` the rate drops to 122.
- Hence the model: sweep a window of **every órgão** (committee votes are most of a deputado's
  work), keep only the nominal ones, store `Voto` rows, and read by deputado from the index. The
  first profile opened pays for the sweep, and every other profile in the term reads it for free.
- A committee vote's **substance** is in `ultimaApresentacaoProposicao.descricao` (the
  rapporteur and their opinion). The vote's own `descricao` is almost always
  `Aprovado o Parecer.` or `Aprovada a Redação Final.` and `aprovacao = 1`. The rapporteur's name
  and party sit **inside free text**, not in fields.

## Proposições: a window, not a term

- There is no "proposições of the term". What exists is a window of about 3 months at the end of
  the term, by filing date.
- **Types cannot be fully listed.** There are 544 siglas and new ones appear, so the app groups
  them into four buckets, and TRAMITACAO is defined as "none of the others" (`NOT IN`). The
  counts are very uneven: 1 PEC against 8848 procedural items in one window.
- **Authors are not always deputados.** A third of PLs are signed by an órgão, and every MSC by
  the Executive. Store the name and type from `/autores`. Don't resolve the author only through
  the deputado table. The first signature is the proponent, the rest are support (a PEC has 1
  name plus 171 others).
- `keywords` is one comma-separated string, not a list. `temas` arrives months late.
- The rapporteur only comes as a URI (`uriUltimoRelator`). The id at the end resolves against
  the local roster, at no cost.

## Ids and URIs

- Ids are numbers in some DTOs and strings in others (`PartidoDto.id: Int`,
  `DeputadoDto.id: String`, votação ids like `2645346-18`). The database stores them as TEXT.
- Many references only come as a URI (`uri`, `uriPartido`, `uriUltimoRelator`,
  `uriProposicaoObjeto`). The id is `uri.substringAfterLast('/')`.
- The nested voter in `/votacoes/{id}/votos` is named `deputado_`, **with a trailing
  underscore**. It is the only field in the API like that.

## Cache freshness depends on the term

A finished term never changes, so its data never expires. That is what makes browsing old terms
cost the network only once. A live term expires by content: votes after 6 h, compositions and
presidencies after 7 days, the committee seats index after a week. See
[../architecture.md](../architecture.md#data-pipeline) and `isCacheFresh`.
