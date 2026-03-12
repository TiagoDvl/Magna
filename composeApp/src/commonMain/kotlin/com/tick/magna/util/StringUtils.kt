package com.tick.magna.util

/**
 * Normalizes a string for search comparison:
 * - Converts to lowercase
 * - Strips common Latin diacritics (accents, cedilla, tilde, etc.)
 *
 * Covers the full set of Portuguese/Spanish diacritics used in Brazilian deputies' names.
 * Implemented in commonMain to avoid platform-specific Normalizer/NSString APIs.
 */
internal fun String.normalizeForSearch(): String = this
    .lowercase()
    .replace('á', 'a').replace('à', 'a').replace('â', 'a').replace('ã', 'a').replace('ä', 'a')
    .replace('é', 'e').replace('è', 'e').replace('ê', 'e').replace('ë', 'e')
    .replace('í', 'i').replace('ì', 'i').replace('î', 'i').replace('ï', 'i')
    .replace('ó', 'o').replace('ò', 'o').replace('ô', 'o').replace('õ', 'o').replace('ö', 'o')
    .replace('ú', 'u').replace('ù', 'u').replace('û', 'u').replace('ü', 'u')
    .replace('ç', 'c').replace('ñ', 'n')
