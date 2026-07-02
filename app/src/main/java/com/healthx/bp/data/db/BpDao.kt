package com.healthx.bp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BpDao {

    // ---- Display queries: live records only (tombstones hidden) ----

    @Query("SELECT * FROM bp_record WHERE deleted = 0 ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<BpRecord>>

    @Query("SELECT * FROM bp_record WHERE deleted = 0 ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(): Flow<BpRecord?>

    @Query("SELECT * FROM bp_record WHERE deleted = 0 AND timestamp >= :from ORDER BY timestamp ASC")
    fun observeSince(from: Long): Flow<List<BpRecord>>

    @Query("SELECT * FROM bp_record WHERE deleted = 0 AND timestamp >= :from AND timestamp <= :to ORDER BY timestamp ASC")
    suspend fun rangeAsc(from: Long, to: Long): List<BpRecord>

    @Query("SELECT * FROM bp_record WHERE deleted = 0 ORDER BY timestamp ASC")
    suspend fun getAllAsc(): List<BpRecord>

    @Query("SELECT COUNT(*) FROM bp_record WHERE deleted = 0")
    fun observeCount(): Flow<Int>

    // ---- Sync queries: raw rows incl. tombstones ----

    @Query("SELECT * FROM bp_record")
    suspend fun getAllRaw(): List<BpRecord>

    @Query("SELECT * FROM bp_record WHERE uid = :uid LIMIT 1")
    suspend fun findByUid(uid: String): BpRecord?

    @Query("SELECT * FROM bp_record WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): BpRecord?

    @Query("UPDATE bp_record SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long)

    // ---- Writes ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BpRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<BpRecord>)

    @Update
    suspend fun update(record: BpRecord)

    @Query("DELETE FROM bp_record")
    suspend fun clear()
}
