package com.tick.magna.data.source.local.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.tick.magna.GetPartidos
import com.tick.magna.MagnaDatabase
import com.tick.magna.Partido
import com.tick.magna.PartidoFavoritoQueries
import com.tick.magna.PartidoQueries
import com.tick.magna.data.dispatcher.DispatcherInterface
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalTime::class)
internal class PartidoDao(
    private val database: MagnaDatabase,
    private val partidoQueries: PartidoQueries,
    private val partidoFavoritoQueries: PartidoFavoritoQueries,
    private val dispatcherInterface: DispatcherInterface
): PartidoDaoInterface {

    override suspend fun insertPartidos(deputadosDetails: List<Partido>) {
        database.transaction {
            deputadosDetails.forEach {
                partidoQueries.insertPartido(
                    id = it.id,
                    legislaturaId = it.legislaturaId,
                    liderDeputadoId = it.liderDeputadoId,
                    sigla = it.sigla,
                    nome = it.nome,
                    situacao = it.situacao,
                    totalPosse = it.totalPosse,
                    totalMembros = it.totalMembros,
                    logo = it.logo,
                    website = it.website
                )
            }
        }
    }

    override suspend fun getPartidos(legislaturaId: String): Flow<List<GetPartidos>?> {
        return partidoQueries
            .getPartidos(legislaturaId)
            .asFlow()
            .mapToList(dispatcherInterface.io)
    }

    override fun observeIsFavorito(partidoId: String): Flow<Boolean> {
        return partidoFavoritoQueries
            .isPartidoFavorito(partidoId)
            .asFlow()
            .mapToOne(dispatcherInterface.io)
    }

    override suspend fun setFavorito(partidoId: String, favorito: Boolean) {
        if (favorito) {
            partidoFavoritoQueries.favoritePartido(
                partidoId = partidoId,
                favoritedAt = Clock.System.now().toEpochMilliseconds(),
            )
        } else {
            partidoFavoritoQueries.unfavoritePartido(partidoId)
        }
    }

    override suspend fun getPartido(legislaturaId: String, partidoId: String): Flow<Partido> {
        return partidoQueries
            .getPartido(legislaturaId, partidoId)
            .asFlow()
            .mapToOne(dispatcherInterface.io)
    }
}