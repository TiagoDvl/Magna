package com.tick.magna.features.partidos.list

import kotlin.test.Test
import kotlin.test.assertEquals

class ReordenacaoTest {

    @Test
    fun `one row of travel is one position`() {
        assertEquals(3, alvoDoArrasto(atual = 2, deslocamento = 72f, altura = 72f, tamanho = 22))
        assertEquals(0, alvoDoArrasto(atual = 2, deslocamento = -144f, altura = 72f, tamanho = 22))
    }

    @Test
    fun `half a row is not a move yet`() {
        // Rounded, not truncated: a row swaps once it has travelled past the midpoint of the
        // next one, which is where the eye expects the gap to open.
        assertEquals(2, alvoDoArrasto(2, deslocamento = 35f, altura = 72f, tamanho = 22))
        assertEquals(3, alvoDoArrasto(2, deslocamento = 37f, altura = 72f, tamanho = 22))
    }

    @Test
    fun `dragging past the end parks at the end`() {
        assertEquals(21, alvoDoArrasto(20, deslocamento = 5000f, altura = 72f, tamanho = 22))
        assertEquals(0, alvoDoArrasto(1, deslocamento = -5000f, altura = 72f, tamanho = 22))
    }

    @Test
    fun `an unmeasured row height moves nothing`() {
        // The first frame of a gesture can arrive before layout has a height to divide by, and
        // dividing by zero would ask for an index that does not exist.
        assertEquals(4, alvoDoArrasto(4, deslocamento = 100f, altura = 0f, tamanho = 22))
        assertEquals(0, alvoDoArrasto(0, deslocamento = 100f, altura = 72f, tamanho = 0))
    }

    @Test
    fun `moving takes the element out and puts it back in`() {
        assertEquals(listOf("b", "c", "a"), mover(listOf("a", "b", "c"), de = 0, para = 2))
        assertEquals(listOf("c", "a", "b"), mover(listOf("a", "b", "c"), de = 2, para = 0))
    }

    @Test
    fun `a move that goes nowhere returns the same list`() {
        val lista = listOf("a", "b", "c")

        assertEquals(lista, mover(lista, de = 1, para = 1))
        assertEquals(lista, mover(lista, de = 1, para = 9))
        assertEquals(lista, mover(lista, de = -1, para = 0))
    }
}
