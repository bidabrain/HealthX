package com.healthx.bp.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A single blood-pressure measurement.
 *
 * Sync fields:
 * - [uid]: cross-device unique id (UUID). Sync merges by this, never by [id].
 * - [updatedAt]: last change time (ms); newer wins on conflict.
 * - [deleted]: tombstone flag (soft delete) so removals propagate across devices.
 *
 * [id] is the local Room primary key only and is NOT serialized into the sync
 * file (it differs per device).
 */
@Serializable
@Entity(tableName = "bp_record", indices = [Index(value = ["uid"], unique = true)])
data class BpRecord(
    @Transient @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String = "",
    val timestamp: Long,
    val systolic: Int,
    val diastolic: Int,
    val heartRate: Int,
    val status: String,
    val note: String = "",
    val updatedAt: Long = 0,
    val deleted: Boolean = false
)
