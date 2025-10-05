package com.tick.magna.data.source.local

expect class DatabaseDriverFactory {
    fun createDriver(): Int
}