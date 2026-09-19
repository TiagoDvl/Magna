package com.tick.magna.data.repository.votos

import com.tick.magna.VotacaoNominal as VotacaoNominalEntity
import com.tick.magna.data.source.remote.csv.parseCsvLine
import com.tick.magna.data.source.remote.csv.withoutBom

/**
 * Reads `votacoes-{ano}.csv`, which is what says *what was voted on*.
 *
 * The vote file has no description in it — it carries `idVotacao` and nothing about the
 * votacao — so an import of the year needs this one alongside it. At 4.28 MB against the vote
 * file's 16.4 MB it is the cheap half, and it is also better than the API at this: it covers
 * every orgao, so the 29 committee votacoes of 2026 that the plenary sweep leaves behind come
 * in for free, next to the 123 from the plenary.
 *
 * It carries the discriminator the API does not expose anywhere. `votosSim`, `votosNao` and
 * `votosOutros` are filled on exactly the 152 nominal votacoes of 2026 and on none of the 7208
 * symbolic ones — no guessing from the description text, which gets 147 of them and two it
 * should not.
 */
internal class VotacoesCsvParser {

    /**
     * Only the nominal votacoes, because only they have individual votes to hang off them.
     *
     * A row here with no rows in `Voto` would claim a symbolic votacao has a record per
     * person, which is the one thing this feature must not imply.
     */
    fun parse(lines: Sequence<String>, legislaturaId: String): Sequence<VotacaoNominalEntity> {
        val iterator = lines.iterator()
        if (!iterator.hasNext()) return emptySequence()

        val columns = parseCsvLine(iterator.next().withoutBom())
            .withIndex()
            .associate { (index, name) -> name to index }

        val id = columns[COLUMN_ID] ?: return emptySequence()
        val descricao = columns[COLUMN_DESCRICAO] ?: return emptySequence()
        val sim = columns[COLUMN_SIM] ?: return emptySequence()
        val nao = columns[COLUMN_NAO] ?: return emptySequence()
        val outros = columns[COLUMN_OUTROS] ?: return emptySequence()
        val dataHora = columns[COLUMN_DATA]
        val orgao = columns[COLUMN_ORGAO]
        val aprovacao = columns[COLUMN_APROVACAO]
        val proposicao = columns[COLUMN_PROPOSICAO]

        return iterator.asSequence().mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null

            val fields = parseCsvLine(line)
            if (fields.size < columns.size) return@mapNotNull null

            val temVotos = listOf(sim, nao, outros).any { fields[it].toIntOrNull()?.let { n -> n > 0 } == true }
            if (!temVotos) return@mapNotNull null

            VotacaoNominalEntity(
                id = fields[id],
                legislaturaId = legislaturaId,
                dataHoraRegistro = dataHora?.let { fields[it] }?.takeIf { it.isNotBlank() },
                descricao = fields[descricao],
                siglaOrgao = orgao?.let { fields[it] }?.takeIf { it.isNotBlank() },
                aprovacao = if (aprovacao?.let { fields[it] } == "1") APPROVED else NOT_APPROVED,
                // Filled on 84 of the 152 nominal votacoes of 2026. The rest keep a null and
                // the card simply has no proposicao to open, which is honest.
                proposicaoId = proposicao?.let { fields[it] }?.takeIf { it.isNotBlank() && it != "0" },
                // Neither label nor ementa is in this file. The sweep fills them in from the
                // API detail when it passes over the same votacao.
                proposicaoRotulo = null,
                proposicaoEmenta = null,
            )
        }
    }

    private companion object {
        const val COLUMN_ID = "id"
        const val COLUMN_DATA = "dataHoraRegistro"
        const val COLUMN_ORGAO = "siglaOrgao"
        const val COLUMN_APROVACAO = "aprovacao"
        const val COLUMN_DESCRICAO = "descricao"
        const val COLUMN_SIM = "votosSim"
        const val COLUMN_NAO = "votosNao"
        const val COLUMN_OUTROS = "votosOutros"
        const val COLUMN_PROPOSICAO = "ultimaApresentacaoProposicao_idProposicao"

        const val APPROVED = 1L
        const val NOT_APPROVED = 0L
    }
}
