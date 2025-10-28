package com.tick.magna.data.source.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.tick.magna.MagnaDatabase

actual class DatabaseDriverFactory(private val context: Context) {

    actual fun createDriver(): SqlDriver {
        println("Tiago - createDriver")
        return AndroidSqliteDriver(MagnaDatabase.Schema, context, "magna.db")
    }
}