package com.tick.magna.data.source.local.dao

import com.tick.magna.DeputadoBio
import com.tick.magna.DeputadoBioQueries
import com.tick.magna.MagnaDatabase

class DeputadoBioDao(
    private val database: MagnaDatabase,
    private val deputadoBioQueries: DeputadoBioQueries,
) : DeputadoBioDaoInterface {

    override suspend fun getBios(deputadoIds: List<String>): List<DeputadoBio> {
        if (deputadoIds.isEmpty()) return emptyList()

        return deputadoBioQueries.getDeputadoBios(deputadoIds).executeAsList()
    }

    override suspend fun insertBios(bios: List<DeputadoBio>) {
        if (bios.isEmpty()) return

        database.transaction {
            bios.forEach { deputadoBioQueries.insertDeputadoBio(it) }
        }
    }
}
