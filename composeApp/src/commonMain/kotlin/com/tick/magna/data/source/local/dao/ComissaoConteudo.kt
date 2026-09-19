package com.tick.magna.data.source.local.dao

/**
 * The three things a committee screen downloads, each cached and refreshed on its own.
 *
 * They are separate because they cost and change differently: the votes are the expensive one
 * and the only one that moves week to week, the composition is renewed once a legislative
 * year, and the presidency changes when a committee elects a new president.
 *
 * The name is what goes in the database, so renaming one of these is a migration.
 */
enum class ComissaoConteudo { VOTACOES, COMPOSICAO, PRESIDENCIA }
