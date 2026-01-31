package com.tick.magna.features.proposicoes.component

import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.recent_proposicoes_section_filter_1_label
import magna.composeapp.generated.resources.recent_proposicoes_section_filter_2_label
import magna.composeapp.generated.resources.recent_proposicoes_section_filter_3_label
import org.jetbrains.compose.resources.StringResource

enum class ProposicaoType {
    PEC, MPV, PLP;

    fun getProposicaoLabel(): StringResource {
        return when (this) {
            PEC -> Res.string.recent_proposicoes_section_filter_1_label
            MPV -> Res.string.recent_proposicoes_section_filter_2_label
            PLP -> Res.string.recent_proposicoes_section_filter_3_label
        }
    }
}
