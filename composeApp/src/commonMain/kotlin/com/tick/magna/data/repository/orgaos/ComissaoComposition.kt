package com.tick.magna.data.repository.orgaos

import com.tick.magna.data.domain.MembroComissao

/**
 * Which of the rows a window returns are the committee's composition on [reference].
 *
 * `/orgaos/{id}/membros` returns every seat that overlaps the window, not the seats held at
 * its end, so a three-month window over the CCJC comes back with 149 rows for a committee of
 * 130 people: the nineteen extra are seats that ended inside it. A seat counts when it had not
 * ended by the reference date, and `dataFim` is null exactly while it is still held.
 *
 * Measured against the composition the endpoint returns when asked with no dates at all —
 * which is the current one, and the only thing there is to check against — this reproduces
 * all 130 of the CCJC with nothing missing and nothing extra. It also answers for a term that
 * ended, where the no-dates form cannot: asked about the last quarter of the 56th it returns
 * that committee as it stood, president included.
 *
 * Duplicates are real and are not an accident of paging. The president of the CSSF is listed
 * twice, once as `Presidente` and once as `Titular`, while the president of the CCJC is listed
 * only as president. Sorting by `codTitulo` before dropping repeats keeps the office rather
 * than the seat, so the mesa never loses somebody to their own second row.
 */
internal fun comissaoComposition(
    membros: List<MembroComissao>,
    reference: String,
): List<MembroComissao> {
    return membros
        .filter { membro -> membro.dataFim == null || membro.dataFim >= reference }
        .sortedWith(compareBy({ it.codTitulo }, { it.nome }))
        .distinctBy { it.deputadoId }
}
