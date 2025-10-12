package com.tick.magna.data.source.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.tick.magna.MagnaDatabase
import java.util.Properties

actual class DatabaseDriverFactory {

    actual fun createDriver(): SqlDriver {
        val driver: SqlDriver = JdbcSqliteDriver(
            url = "jdbc:sqlite:magna.db",
            properties = Properties(),
            schema = MagnaDatabase.Schema
        )
        return driver
    }
}