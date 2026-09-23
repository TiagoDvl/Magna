package com.tick.magna.data.source.local

import android.content.Context
import android.database.sqlite.SQLiteException
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.tick.magna.MagnaDatabase
import com.tick.magna.data.logger.AppLoggerInterface

actual class DatabaseDriverFactory(
    private val context: Context,
    private val logger: AppLoggerInterface,
) {

    /**
     * Opens the database, and starts over from an empty one if it cannot be opened.
     *
     * **This deletes everything on disk when a migration fails**, including the santinho and the
     * preferences, which are the only rows here the API cannot give back. That was a deliberate
     * choice: an app that crashes on launch every time loses the person entirely, and an app
     * that opens empty loses them a cache and a few notes. The failure goes to Crashlytics as a
     * non-fatal so the broken migration still gets found and fixed rather than silently absorbed.
     *
     * The query is what makes this work at all. `AndroidSqliteDriver` opens the file lazily, so
     * the migrations run on the first statement and not in the constructor — without forcing it
     * here, the exception would surface later, in whichever DAO happened to query first.
     *
     * The whole file goes rather than table by table: a migration that failed halfway can leave
     * the schema in a state that no list of `DROP`s written today anticipates.
     *
     * Only a failure to open is covered. A migration that succeeds but leaves the schema wrong
     * still crashes later, at the query that trips over it.
     */
    actual fun createDriver(): SqlDriver {
        val driver = abrir()

        return try {
            driver.executeQuery(null, "PRAGMA user_version", { QueryResult.Unit }, 0)
            driver
        } catch (e: SQLiteException) {
            logger.e("Could not open $NOME, starting over from an empty database", e, TAG)
            driver.close()
            context.deleteDatabase(NOME)
            abrir()
        }
    }

    private fun abrir(): SqlDriver = AndroidSqliteDriver(
        schema = MagnaDatabase.Schema,
        context = context,
        name = NOME,
    )

    private companion object {
        const val NOME = "magna.db"
        const val TAG = "DatabaseDriverFactory"
    }
}
