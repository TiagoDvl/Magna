package com.tick.magna.features.deputados.search

/**
 * The five regions, as a filter that costs nothing to have.
 *
 * It is derived from the UF that is already on every row — twenty-seven siglas mapped to five
 * names, written once — so it adds a way of thinking about the house without adding a byte of
 * data or a single request. Measured against the 513 in exercise: Sudeste 179, Nordeste 151,
 * Sul 77, Norte 65, Centro-Oeste 41.
 */
enum class Regiao(val label: String, val ufs: Set<String>) {
    NORTE("Norte", setOf("AC", "AM", "AP", "PA", "RO", "RR", "TO")),
    NORDESTE("Nordeste", setOf("AL", "BA", "CE", "MA", "PB", "PE", "PI", "RN", "SE")),
    CENTRO_OESTE("Centro-Oeste", setOf("DF", "GO", "MS", "MT")),
    SUDESTE("Sudeste", setOf("ES", "MG", "RJ", "SP")),
    SUL("Sul", setOf("PR", "RS", "SC")),
    ;

    companion object {
        /**
         * @return null for a UF outside the twenty-seven, which the filter then never matches.
         * A deputado with no state at all is the same case.
         */
        fun de(uf: String?): Regiao? {
            val sigla = uf?.trim()?.uppercase() ?: return null

            return entries.firstOrNull { sigla in it.ufs }
        }

        fun porLabel(label: String): Regiao? = entries.firstOrNull { it.label == label }
    }
}
