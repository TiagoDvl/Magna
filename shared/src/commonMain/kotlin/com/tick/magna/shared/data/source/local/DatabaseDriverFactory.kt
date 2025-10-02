package com.tick.magna.shared.data.source.local

expect class DatabaseDriverFactory {
    fun createDriver(): Int
}