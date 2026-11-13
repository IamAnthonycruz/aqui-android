package io.github.iamanthonycruz.aqui

import android.content.Context
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/** What happened for one DWELL event. Stored as the lowercase [code]. */
enum class PingResult(val code: String) {
    SENT("sent"),
    SKIPPED_SAME_SPOT("skipped_same_spot"),
    SKIPPED_MOVING("skipped_moving"),
    SEND_FAILED("send_failed");

    companion object {
        fun fromCode(code: String) = entries.first { it.code == code }
    }
}

/** One row per DWELL event. Lives only on this phone (backups are off). */
@Entity(tableName = "ping_events")
data class PingEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyMeters: Float?,
    val speedMps: Float?,
    val result: PingResult,
    /** The text that went out, or tried to (kept when it failed, to see why). Null for skips. */
    val message: String?,
    /** Who this row is about ([Contact.id]). Null only for rows from before people existed with no number saved. */
    val contactId: Long? = null,
    /**
     * Why the phone said the text failed: SmsManager's RESULT_ERROR_* / RESULT_RIL_* code
     * (1 = generic failure, 4 = no service, ...). Null unless [result] is SEND_FAILED.
     */
    val errorCode: Int? = null,
) {
    fun toSentPing() = SentPing(
        timeMillis = timestamp,
        spot = if (latitude != null && longitude != null) Spot(latitude, longitude) else null,
    )
}

class PingResultConverter {
    @TypeConverter fun toCode(r: PingResult): String = r.code
    @TypeConverter fun fromCode(code: String): PingResult = PingResult.fromCode(code)
}

@Dao
interface PingEventDao {
    /** Returns the new row's id. */
    @Insert
    fun insert(event: PingEvent): Long

    /** The phone reported a part of this text didn't go out. Values must match [PingResult]. */
    @Query("UPDATE ping_events SET result = 'send_failed', errorCode = :errorCode WHERE id = :id")
    fun markSendFailed(id: Long, errorCode: Int?)

    /**
     * The last text that went out to this person. Failed sends and skips don't count: only a text
     * that really went out starts their cooldown. 'sent' must match [PingResult.SENT].code.
     */
    @Query("SELECT * FROM ping_events WHERE result = 'sent' AND contactId = :contactId ORDER BY timestamp DESC LIMIT 1")
    fun lastSentTo(contactId: Long): PingEvent?

    /** Each person's latest row since [since] (the stay's start). See [People.handled]. */
    @Query("SELECT contactId, MAX(timestamp) AS at FROM ping_events WHERE timestamp >= :since AND contactId IS NOT NULL GROUP BY contactId")
    fun lastEventSince(since: Long): List<ContactEventTime>

    /** Rows since [since], updated live. The Start screen's "Texted 3:42". */
    @Query("SELECT * FROM ping_events WHERE timestamp >= :since ORDER BY timestamp")
    fun since(since: Long): Flow<List<PingEvent>>

    /** See [Retention.eventsCutoff]. */
    @Query("DELETE FROM ping_events WHERE timestamp < :before")
    fun deleteOlderThan(before: Long)
}

data class ContactEventTime(val contactId: Long, val at: Long)

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY id")
    fun all(): Flow<List<Contact>>

    /** For the tracker's background thread. */
    @Query("SELECT * FROM contacts ORDER BY id")
    fun allNow(): List<Contact>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun byId(id: Long): Contact?

    /** Returns the new person's id. */
    @Insert
    suspend fun insert(c: Contact): Long

    @Update
    suspend fun update(c: Contact)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun delete(id: Long)

}

/**
 * One row per location fix the tracker got (about one a minute while running).
 * The trace of what the phone really did, so a missed ping can be explained afterwards.
 */
@Entity(tableName = "location_fixes")
data class LocationFix(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val speedMps: Float?,
    /** Minutes at the current stay after this fix. Null = ignored, too inaccurate to count. */
    val minutesHere: Int?,
)

@Dao
interface LocationFixDao {
    @Insert
    fun insert(fix: LocationFix)

    @Query("DELETE FROM location_fixes WHERE timestamp < :before")
    fun deleteOlderThan(before: Long)
}

@Database(entities = [PingEvent::class, LocationFix::class, Contact::class], version = 1, exportSchema = false)
@TypeConverters(PingResultConverter::class)
abstract class PingLogDatabase : RoomDatabase() {
    abstract fun pingEvents(): PingEventDao
    abstract fun locationFixes(): LocationFixDao
    abstract fun contacts(): ContactDao
}


/** The on-device ping log, fix trace, and people. Call the non-suspend functions from a background thread. */
object PingLog {
    @Volatile private var db: PingLogDatabase? = null

    private fun db(c: Context): PingLogDatabase = db ?: synchronized(this) {
        val app = c.applicationContext
        db ?: Room.databaseBuilder(app, PingLogDatabase::class.java, "ping_log.db")
            .addCallback(IncrementalVacuum)
            .build().also { db = it }
    }

    fun dao(c: Context): PingEventDao = db(c).pingEvents()
    fun fixes(c: Context): LocationFixDao = db(c).locationFixes()
    fun contacts(c: Context): ContactDao = db(c).contacts()

    /**
     * Deletes fixes and ping rows past [Retention]'s cutoffs, then gives the freed space back to the
     * phone so the file shrinks instead of only growing. [staySinceMillis] = the current stay's start.
     */
    fun prune(c: Context, now: Long, staySinceMillis: Long) {
        val db = db(c)
        db.runInTransaction {
            db.locationFixes().deleteOlderThan(Retention.fixesCutoff(now))
            db.pingEvents().deleteOlderThan(Retention.eventsCutoff(now, staySinceMillis))
        }
        // Returns no rows, but it only runs while stepped, so drain the cursor.
        db.openHelper.writableDatabase.query("PRAGMA incremental_vacuum").use { while (it.moveToNext()) Unit }
    }

    /**
     * Turns on auto_vacuum = INCREMENTAL, so [prune] can hand freed pages back. SQLite only applies
     * it after a VACUUM, so the first open (new install, or a file from an older version) runs one.
     */
    private object IncrementalVacuum : RoomDatabase.Callback() {
        private const val INCREMENTAL = 2

        override fun onOpen(db: SupportSQLiteDatabase) {
            val mode = db.query("PRAGMA auto_vacuum").use { if (it.moveToFirst()) it.getInt(0) else -1 }
            if (mode != INCREMENTAL) {
                // Housekeeping only: if it fails, the app works the same, and it's tried again next open.
                try {
                    db.execSQL("PRAGMA auto_vacuum = INCREMENTAL")
                    db.execSQL("VACUUM")
                } catch (e: Exception) {
                    Log.e("Aqui", "Turning on incremental vacuum failed", e)
                }
            }
        }
    }
}
