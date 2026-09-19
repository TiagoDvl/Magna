package com.tick.magna.data.repository.orgaos

import com.tick.magna.data.domain.MembroComissao
import com.tick.magna.data.domain.Orgao
import com.tick.magna.data.domain.Votacao
import kotlinx.coroutines.flow.Flow

interface OrgaosRepositoryInterface {

    suspend fun syncComissoesPermanentes(): Boolean

    fun getComissoesPermanentes(): Flow<List<Orgao>>

    /**
     * Whether the table was ever filled, which is not the same question as whether the list
     * has anything in it. The list is filtered by the selected term and is legitimately empty
     * for terms that predate every committee.
     */
    suspend fun hasComissoesPermanentes(): Boolean

    /**
     * Whether the selected term still has committees whose activity was never counted.
     *
     * Asked by the sync, because having the committee rows is not the same as being able to
     * order them, and somebody upgrading already has the rows.
     */
    suspend fun needsAtividade(): Boolean

    suspend fun getComissaoPermanenteVotacoes(idOrgao: String): Result<List<Votacao>>

    /**
     * Who sits on the committee, as of the end of the selected term or of today, whichever
     * came first. Ordered president first, then the rest of the mesa, then titulares, then
     * suplentes, and alphabetically inside each.
     */
    suspend fun getComissaoMembros(idOrgao: String): Result<List<MembroComissao>>

    /**
     * Everybody who presided over the committee during the selected term, most recent first.
     *
     * The expensive one: it reads the whole mandate, which is ten requests for the CCJC and
     * two for most committees. Asked only when somebody opens the tab that shows it.
     */
    suspend fun getComissaoPresidentes(idOrgao: String): Result<List<MembroComissao>>
}