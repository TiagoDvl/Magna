package com.tick.magna.data.repository.votos

import com.tick.magna.Voto as VotoEntity
import com.tick.magna.data.source.remote.csv.parseCsvLine
import com.tick.magna.data.source.remote.csv.withoutBom

/**
 * Reads `votacoesVotos-{ano}.csv` into the rows the vote index stores.
 *
 * This file is the only place the Camara publishes the relation the API refuses to invert.
 * `/deputados/{id}/votos` is a 405; the file has one line per person per votacao and is 16.4 MB
 * for 2026, 51832 lines.
 *
 * **Columns are read by name, never by position.** The plan for this block listed eleven of
 * them and the file has twelve — `deputado_uri` sits between `deputado_id` and `deputado_nome`.
 * A positional parser written from that list would have loaded every deputado's URI into the
 * name column and looked like it worked.
 *
 * Feeding it a sequence rather than a string is the point: the file does not fit comfortably
 * in memory on a phone, and nothing here needs more than one line at a time.
 */
internal class VotosCsvParser {

    /**
     * Parses lines into vote rows, skipping what cannot be stored.
     *
     * Two things are dropped, and both are real rather than defensive:
     *
     * A line whose `voto` is blank. Votacao 2645346-18 of 2026 has 466 of them and is the only
     * votacao in the file that does — a nominal votacao whose individual record was never
     * filled in. The API agrees, returning a null `tipoVoto` for the same 466. A vote with no
     * vote in it is not information.
     *
     * A line with the wrong number of fields, which none of the 24082 sampled had. It is here
     * so that a file that changes shape truncates the import instead of writing nonsense into
     * the index.
     */
    fun parse(lines: Sequence<String>, legislaturaId: String): Sequence<VotoEntity> {
        val iterator = lines.iterator()
        if (!iterator.hasNext()) return emptySequence()

        val columns = parseCsvLine(iterator.next().withoutBom())
            .withIndex()
            .associate { (index, name) -> name to index }

        val idVotacao = columns[COLUMN_VOTACAO] ?: return emptySequence()
        val voto = columns[COLUMN_VOTO] ?: return emptySequence()
        val deputadoId = columns[COLUMN_DEPUTADO] ?: return emptySequence()
        val dataHoraVoto = columns[COLUMN_DATA]

        return iterator.asSequence().mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null

            val fields = parseCsvLine(line)
            if (fields.size < columns.size) return@mapNotNull null

            val tipoVoto = fields[voto]
            if (tipoVoto.isBlank()) return@mapNotNull null

            VotoEntity(
                votacaoId = fields[idVotacao],
                deputadoId = fields[deputadoId],
                legislaturaId = legislaturaId,
                voto = tipoVoto,
                dataHoraVoto = dataHoraVoto?.let { fields[it] }?.takeIf { it.isNotBlank() },
            )
        }
    }

    private companion object {
        const val COLUMN_VOTACAO = "idVotacao"
        const val COLUMN_VOTO = "voto"
        const val COLUMN_DEPUTADO = "deputado_id"
        const val COLUMN_DATA = "dataHoraVoto"
    }
}
