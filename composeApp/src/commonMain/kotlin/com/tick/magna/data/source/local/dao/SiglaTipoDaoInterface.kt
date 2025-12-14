package com.tick.magna.data.source.local.dao

import com.tick.magna.SiglaTipo

interface SiglaTipoDaoInterface {

    fun insertSiglaTipos(siglaTipos: List<SiglaTipo>)

    fun getSiglaTipoById(siglaTipoId: String): SiglaTipo
}