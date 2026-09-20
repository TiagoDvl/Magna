package com.tick.magna.features.partidos.list

import kotlin.math.roundToInt

/**
 * Where a row being dragged should sit, given how far it has travelled.
 *
 * Rows are a fixed height on this screen, which is what lets this be arithmetic instead of a
 * walk over the laid-out items: one row of travel is one position. The alternative reads
 * `layoutInfo.visibleItemsInfo` on every pointer event and has to handle the rows that are
 * off screen, which for twenty-two parties buys nothing.
 *
 * Clamped to the list, so dragging past either end parks the row at that end rather than
 * asking for an index that does not exist.
 */
internal fun alvoDoArrasto(atual: Int, deslocamento: Float, altura: Float, tamanho: Int): Int {
    if (tamanho == 0 || altura <= 0f) return atual

    return (atual + (deslocamento / altura).roundToInt()).coerceIn(0, tamanho - 1)
}

/**
 * The same list with one element moved.
 *
 * Returns the list unchanged when the move is a no-op or out of bounds, so a caller can apply
 * the result without checking first.
 */
internal fun <T> mover(lista: List<T>, de: Int, para: Int): List<T> {
    if (de == para) return lista
    if (de !in lista.indices || para !in lista.indices) return lista

    return lista.toMutableList().apply { add(para, removeAt(de)) }
}
