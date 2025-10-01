package com.tick.magna

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform