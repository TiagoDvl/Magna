package com.tick.magna.data.source.local

import android.content.Context
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.tick.magna.MagnaDatabase

actual class DatabaseDriverFactory(private val context: Context) {

    actual fun createDriver(): SqlDriver {
        context.deleteDatabase("magna.db")
        return AndroidSqliteDriver(
            MagnaDatabase.Schema,
            context,
            "magna.db",
            callback = object: AndroidSqliteDriver.Callback(MagnaDatabase.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    Log.d("SQLDelight", "Database opened: ${db.path}")
                    Log.d("SQLDelight", "Database version: ${db.version}")
                    inspectDatabase(context)
                }

                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    Log.d("SQLDelight", "Database created")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    Log.d("SQLDelight", "Database upgrade from $oldVersion to $newVersion")
                    super.onUpgrade(db, oldVersion, newVersion)
                }

                override fun onCorruption(db: SupportSQLiteDatabase) {
                    Log.e("SQLDelight", "Database corruption detected!")
                    super.onCorruption(db)
                }
            }
        )
    }

    private fun inspectDatabase(context: Context) {
        val db = context.openOrCreateDatabase("magna.db", Context.MODE_PRIVATE, null)

        // Lista todas as tabelas
        val tablesCursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table'",
            null
        )
        Log.d("SQLDelight", "=== TABLES ===")
        while (tablesCursor.moveToNext()) {
            val tableName = tablesCursor.getString(0)
            Log.d("SQLDelight", "Table: $tableName")

            // Mostra schema de cada tabela
            val schemaCursor = db.rawQuery("PRAGMA table_info($tableName)", null)
            while (schemaCursor.moveToNext()) {
                val columnName = schemaCursor.getString(1)
                val columnType = schemaCursor.getString(2)
                Log.d("SQLDelight", "  - $columnName: $columnType")
            }
            schemaCursor.close()
        }
        tablesCursor.close()
        db.close()
    }
}