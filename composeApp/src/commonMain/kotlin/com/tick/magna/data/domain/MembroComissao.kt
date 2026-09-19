package com.tick.magna.data.domain

/**
 * One person's seat on a committee, for one stretch of time.
 *
 * The dates are part of the record rather than something the repository strips off, because
 * a seat is not a fact about a person but about a period: the same deputy appears several
 * times over a mandate, and which of those rows is the current one is decided by comparing
 * [dataFim] against a reference date.
 */
data class MembroComissao(
    val deputadoId: String,
    val nome: String,
    val siglaPartido: String?,
    val siglaUf: String?,
    val urlFoto: String?,
    /** As the Camara spells it: `Presidente`, `1º Vice-Presidente`, `Titular`, `Suplente`. */
    val titulo: String,
    /** 1 to 4 for the mesa, 101 titular, 102 suplente. Also the order the mesa is listed in. */
    val codTitulo: Int,
    val dataInicio: String,
    /** Null while the seat is still held. */
    val dataFim: String?,
) {

    val cargo: CargoComissao
        get() = when (codTitulo) {
            in PRESIDENTE..TERCEIRO_VICE -> CargoComissao.MESA
            SUPLENTE -> CargoComissao.SUPLENTE
            else -> CargoComissao.TITULAR
        }

    val isPresidente: Boolean
        get() = codTitulo == PRESIDENTE

    private companion object {
        const val PRESIDENTE = 1
        const val TERCEIRO_VICE = 4
        const val SUPLENTE = 102
    }
}

/**
 * The three groups a committee page is divided into.
 *
 * Deliberately coarser than `codTitulo`. The mesa is four distinct offices, but they are one
 * block on the screen and the titles themselves say which is which.
 */
enum class CargoComissao { MESA, TITULAR, SUPLENTE }

val membrosComissaoMock = listOf(
    MembroComissao(
        deputadoId = "178860",
        nome = "Paulo Azi",
        siglaPartido = "UNIÃO",
        siglaUf = "BA",
        urlFoto = "https://www.camara.leg.br/internet/deputado/bandep/178860.jpg",
        titulo = "Presidente",
        codTitulo = 1,
        dataInicio = "2026-02-09",
        dataFim = null,
    ),
    MembroComissao(
        deputadoId = "204528",
        nome = "Baleia Rossi",
        siglaPartido = "MDB",
        siglaUf = "SP",
        urlFoto = "https://www.camara.leg.br/internet/deputado/bandep/204528.jpg",
        titulo = "1º Vice-Presidente",
        codTitulo = 2,
        dataInicio = "2026-03-17",
        dataFim = null,
    ),
    MembroComissao(
        deputadoId = "204551",
        nome = "Erika Kokay",
        siglaPartido = "PT",
        siglaUf = "DF",
        urlFoto = "https://www.camara.leg.br/internet/deputado/bandep/204551.jpg",
        titulo = "Titular",
        codTitulo = 101,
        dataInicio = "2026-02-04",
        dataFim = null,
    ),
    MembroComissao(
        deputadoId = "220608",
        nome = "Sâmia Bomfim",
        siglaPartido = "PSOL",
        siglaUf = "SP",
        urlFoto = "https://www.camara.leg.br/internet/deputado/bandep/220608.jpg",
        titulo = "Suplente",
        codTitulo = 102,
        dataInicio = "2026-02-04",
        dataFim = null,
    ),
)
