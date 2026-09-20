package com.tick.magna.data.domain

/**
 * The deputado who leads a party's bench.
 *
 * @param id so the row that shows them can open them. The register gives the leader as a URI
 * ending in the deputado's id, and without it the leader was the one person on the party
 * screen you could not tap. Null for a term synced before the id was kept.
 */
data class Lider(
    val id: String?,
    val nome: String,
    val uf: String,
    val urlFoto: String,
)
