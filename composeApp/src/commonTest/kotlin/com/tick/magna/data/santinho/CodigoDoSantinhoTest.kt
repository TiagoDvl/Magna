package com.tick.magna.data.santinho

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodigoDoSantinhoTest {

    @Test
    fun `every note is the same length, full or empty`() {
        // The reason padding exists. AES-GCM ciphertext is as long as its plaintext, so a
        // variable-length note would tell anybody reading the database file how much somebody
        // had written down without decrypting a byte of it.
        val vazio = Santinho().paraTexto()
        val cheio = Santinho("1234", "56789", "123", "12", "13").paraTexto()
        val meio = Santinho(deputadoFederal = "1234").paraTexto()

        assertEquals(TAMANHO, vazio.length)
        assertEquals(TAMANHO, cheio.length)
        assertEquals(TAMANHO, meio.length)
    }

    @Test
    fun `a note survives the round trip`() {
        val original = Santinho("1234", "56789", "123", "12", "13")

        assertEquals(original, santinhoDeTexto(original.paraTexto()))
    }

    @Test
    fun `a half-filled note survives it too`() {
        val original = Santinho(deputadoFederal = "4477", presidente = "13")

        val voltou = santinhoDeTexto(original.paraTexto())

        assertEquals("4477", voltou.deputadoFederal)
        assertEquals("13", voltou.presidente)
        assertEquals("", voltou.senador)
    }

    @Test
    fun `a leading zero is a digit and not padding`() {
        // The padding is a space precisely so that this works: somebody voting for 07 has
        // written down two digits, not one.
        val voltou = santinhoDeTexto(Santinho(presidente = "07").paraTexto())

        assertEquals("07", voltou.presidente)
    }

    @Test
    fun `a text shorter than it should be decodes to what it has`() {
        // What a note written by a version with fewer offices on it looks like. Throwing it
        // away would lose digits somebody typed.
        val voltou = santinhoDeTexto("1234 5678")

        assertEquals("1234", voltou.deputadoFederal)
        assertEquals("5678", voltou.deputadoEstadual)
        assertEquals("", voltou.presidente)
    }

    @Test
    fun `only digits get in, and only as many as the machine shows`() {
        val santinho = Santinho()
            .com(CargoDaUrna.PRESIDENTE, "1a3b5")
            .com(CargoDaUrna.DEPUTADO_FEDERAL, "123456789")

        assertEquals("13", santinho.presidente)
        assertEquals("1234", santinho.deputadoFederal)
    }

    @Test
    fun `an empty note knows it is empty`() {
        assertTrue(Santinho().vazio)
        assertEquals(0, Santinho().preenchidos)
        assertEquals(2, Santinho(senador = "123", presidente = "13").preenchidos)
    }

    private companion object {
        /** Four plus five plus three plus two plus two, which is the ballot. */
        const val TAMANHO = 16
    }
}
