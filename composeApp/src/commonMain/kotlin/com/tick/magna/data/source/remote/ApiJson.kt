package com.tick.magna.data.source.remote

import kotlinx.serialization.json.Json

/**
 * The parser used for every Camara response.
 *
 * Shared with the tests on purpose: a test that builds its own lenient parser proves
 * nothing about what the app does with a real payload.
 *
 * - [Json.ignoreUnknownKeys] lets the API grow fields without breaking the app, and lets
 *   the DTOs declare only what is actually read.
 * - [Json.coerceInputValues] turns an explicit `null` on a field that has a default into
 *   that default. The register is uneven and sends nulls freely; without this, one record
 *   missing a value fails the whole list it arrived in.
 */
internal fun apiJson(prettyPrint: Boolean = false): Json = Json {
    this.prettyPrint = prettyPrint
    isLenient = true
    ignoreUnknownKeys = true
    coerceInputValues = true
}
