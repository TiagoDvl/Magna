package com.tick.magna.util

import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyFormatTest {

    @Test
    fun formats_a_plain_amount_with_two_decimals() {
        assertEquals("R$ 350,75", 350.75.toBrlString())
        assertEquals("R$ 0,00", 0.0.toBrlString())
        assertEquals("R$ 9,90", 9.9.toBrlString())
    }

    @Test
    fun groups_thousands_with_a_dot() {
        assertEquals("R$ 8.000,00", 8000.0.toBrlString())
        assertEquals("R$ 1.234,56", 1234.56.toBrlString())
        assertEquals("R$ 999,99", 999.99.toBrlString())
        assertEquals("R$ 1.000.000,00", 1_000_000.0.toBrlString())
    }

    @Test
    fun groups_every_three_digits_from_the_right() {
        assertEquals("R$ 12.345,00", 12_345.0.toBrlString())
        assertEquals("R$ 123.456,00", 123_456.0.toBrlString())
        assertEquals("R$ 1.234.567,89", 1_234_567.89.toBrlString())
    }

    @Test
    fun rounds_to_the_nearest_cent() {
        assertEquals("R$ 0,01", 0.005.toBrlString())
        assertEquals("R$ 2,35", 2.345.toBrlString())
        assertEquals("R$ 10,00", 9.999.toBrlString())
    }

    @Test
    fun keeps_the_sign_in_front_of_the_currency() {
        assertEquals("-R$ 50,00", (-50.0).toBrlString())
        assertEquals("-R$ 1.500,25", (-1500.25).toBrlString())
    }

    @Test
    fun survives_values_that_are_not_real_numbers() {
        assertEquals("R$ 0,00", Double.NaN.toBrlString())
        assertEquals("R$ 0,00", Double.POSITIVE_INFINITY.toBrlString())
        assertEquals("R$ 0,00", Double.NEGATIVE_INFINITY.toBrlString())
    }

    @Test
    fun never_renders_a_bare_float_the_way_the_old_code_did() {
        // The amount used to be stored and shown as "R$ 8000.0".
        val formatted = 8000.0.toBrlString()

        assertEquals("R$ 8.000,00", formatted)
        assertEquals(false, formatted.contains("8000.0"))
    }
}
