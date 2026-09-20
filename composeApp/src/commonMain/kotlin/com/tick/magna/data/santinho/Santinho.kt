package com.tick.magna.data.santinho

/**
 * The numbers somebody wants to have with them when they vote.
 *
 * **Digits and nothing else.** Not a candidate, not a party, not a name — this app knows about
 * federal deputies and about nothing else on the ballot, and looking a number up against the
 * only list it happens to hold would turn a private note into a record of who the person is
 * voting for. Free text, unvalidated on purpose: the register of candidates is not ours, and a
 * note that argues with what somebody wrote down is a note they will stop trusting.
 *
 * Empty strings are the ordinary state. Somebody who only cares about one race fills in one
 * field, and four blanks is a complete answer.
 */
data class Santinho(
    val deputadoFederal: String = "",
    val deputadoEstadual: String = "",
    val senador: String = "",
    val governador: String = "",
    val presidente: String = "",
) {
    val vazio: Boolean
        get() = campos.all { it.valor.isEmpty() }

    val preenchidos: Int
        get() = campos.count { it.valor.isNotEmpty() }

    val campos: List<CampoDoSantinho>
        get() = CargoDaUrna.entries.map { CampoDoSantinho(it, valorDe(it)) }

    fun valorDe(cargo: CargoDaUrna): String = when (cargo) {
        CargoDaUrna.DEPUTADO_FEDERAL -> deputadoFederal
        CargoDaUrna.DEPUTADO_ESTADUAL -> deputadoEstadual
        CargoDaUrna.SENADOR -> senador
        CargoDaUrna.GOVERNADOR -> governador
        CargoDaUrna.PRESIDENTE -> presidente
    }

    fun com(cargo: CargoDaUrna, valor: String): Santinho {
        val limpo = valor.filter { it.isDigit() }.take(cargo.digitos)

        return when (cargo) {
            CargoDaUrna.DEPUTADO_FEDERAL -> copy(deputadoFederal = limpo)
            CargoDaUrna.DEPUTADO_ESTADUAL -> copy(deputadoEstadual = limpo)
            CargoDaUrna.SENADOR -> copy(senador = limpo)
            CargoDaUrna.GOVERNADOR -> copy(governador = limpo)
            CargoDaUrna.PRESIDENTE -> copy(presidente = limpo)
        }
    }
}

data class CampoDoSantinho(val cargo: CargoDaUrna, val valor: String)

/**
 * The offices on a Brazilian general-election ballot, in the order the machine asks for them.
 *
 * The order matters: the booth asks in this sequence and a note that lists them in another
 * order is a note somebody has to re-read under pressure. [digitos] is how many boxes the
 * machine shows, which is what makes the note look like the thing it stands in for.
 */
enum class CargoDaUrna(val digitos: Int) {
    DEPUTADO_FEDERAL(4),
    DEPUTADO_ESTADUAL(5),
    SENADOR(3),
    GOVERNADOR(2),
    PRESIDENTE(2),
}
