package com.tick.magna.data.santinho

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.tick.magna.data.source.local.dao.Preferencias
import com.tick.magna.data.source.local.dao.PreferenciaDaoInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.tick.magna.SantinhoQueries
import com.tick.magna.data.dispatcher.DispatcherInterface
import kotlinx.coroutines.withContext

interface SantinhoRepositoryInterface {

    /** False where the platform has no keystore. The feature is not offered at all there. */
    val disponivel: Boolean

    /**
     * The note, and again every time it changes.
     *
     * A one-shot read was the bug this replaces: the Home's card and the editing screen each
     * get their own ViewModel, so the card kept showing what it had read when the Home was
     * first composed and went on saying "2 of 5" after the note had been deleted. Both observe
     * the same table now, so a write on one screen is a fact on the other.
     */
    fun observar(): Flow<Santinho>

    /** Whether the person closed the banner on the Home. */
    val bannerDispensado: Flow<Boolean>

    suspend fun dispensarBanner()

    /** False when the note could not be encrypted, which is the only reason to refuse a save. */
    suspend fun guardar(santinho: Santinho, quando: String): Boolean

    suspend fun apagar()
}

/**
 * The note, encrypted on the way in and decrypted on the way out.
 *
 * Nothing here talks to the network, and there is no sync, no upload and no analytics event
 * carrying any part of it. That is not a policy written somewhere else that this file happens
 * to follow — it is the whole of the implementation: one table, one key, one device.
 *
 * A note that will not decrypt comes back empty rather than as an error. The keystore drops a
 * key when the lock screen is removed and recreates a different one afterwards, so "these
 * bytes are no longer readable" is a thing that happens to ordinary people who changed a phone
 * setting, and the only useful thing to show them is a blank form.
 */
internal class SantinhoRepository(
    private val queries: SantinhoQueries,
    private val preferenciaDao: PreferenciaDaoInterface,
    private val cofre: CofreLocalInterface,
    private val dispatcher: DispatcherInterface,
) : SantinhoRepositoryInterface {

    override val disponivel: Boolean get() = cofre.disponivel

    override fun observar(): Flow<Santinho> =
        queries.getSantinho()
            .asFlow()
            .mapToOneOrNull(dispatcher.io)
            .map { linha ->
                val texto = linha?.conteudo?.let(cofre::decifrar) ?: return@map Santinho()

                santinhoDeTexto(texto)
            }

    override val bannerDispensado: Flow<Boolean> =
        preferenciaDao.observarBooleano(Preferencias.SANTINHO_BANNER_DISPENSADO)

    override suspend fun dispensarBanner() {
        withContext(dispatcher.io) {
            preferenciaDao.definirBooleano(Preferencias.SANTINHO_BANNER_DISPENSADO, true)
        }
    }

    override suspend fun guardar(santinho: Santinho, quando: String): Boolean =
        withContext(dispatcher.io) {
            // An emptied note is a deleted note. Storing a blank one would leave a row saying
            // somebody keeps a santinho, which is the one thing the table is meant not to say
            // about a person who decided not to.
            if (santinho.vazio) {
                queries.deleteSantinho()
                return@withContext true
            }

            val cifrado = cofre.cifrar(santinho.paraTexto()) ?: return@withContext false

            queries.upsertSantinho(cifrado, quando)
            true
        }

    override suspend fun apagar() {
        withContext(dispatcher.io) { queries.deleteSantinho() }
    }
}
