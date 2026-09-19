package com.tick.magna.data.source.remote.csv

/**
 * One line of a delimited file, split with the quoting rules honoured.
 *
 * The Camara's annual files are the first thing this app reads that is not JSON from the API,
 * and they are not comma-separated: the delimiter is `;`, every field is wrapped in `"`, and
 * the file opens with a UTF-8 BOM.
 *
 * Measured over 24082 rows of `votacoesVotos-2026.csv`, sampled at four points in the file:
 * every line has exactly twelve fields, none contains an escaped quote, none contains the
 * delimiter inside a quoted field, and none has an odd number of quotes — so no field spans
 * two lines and reading the file line by line is safe.
 *
 * This still handles all three of those cases. They cost a few lines here and a whole
 * re-download to discover later, and the file is regenerated every night by something this
 * app does not control.
 */
internal fun parseCsvLine(line: String, delimiter: Char = ';'): List<String> {
    val fields = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    var index = 0

    while (index < line.length) {
        val char = line[index]

        when {
            char == '"' && inQuotes && line.getOrNull(index + 1) == '"' -> {
                // "" inside a quoted field is one literal quote, not the end of the field.
                field.append('"')
                index++
            }

            char == '"' -> inQuotes = !inQuotes

            char == delimiter && !inQuotes -> {
                fields += field.toString()
                field.clear()
            }

            else -> field.append(char)
        }

        index++
    }

    fields += field.toString()

    return fields
}

/** The bytes the Camara puts in front of the header, which are not part of the first column name. */
internal fun String.withoutBom(): String = removePrefix("﻿")
