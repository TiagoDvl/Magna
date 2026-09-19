package com.tick.magna.data.repository.orgaos

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
}