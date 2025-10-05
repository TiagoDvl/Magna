package com.tick.magna.data.source.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LinkDto(
    val rel: String,
    val href: String
)
