package com.healthx.bp.data.repository

import com.healthx.bp.data.db.BpDao
import com.healthx.bp.data.db.BpRecord
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Single access point for blood-pressure records on top of Room. */
class BpRepository(private val dao: BpDao) {

    fun all(): Flow<List<BpRecord>> = dao.observeAll()
    fun latest(): Flow<BpRecord?> = dao.observeLatest()
    fun since(from: Long): Flow<List<BpRecord>> = dao.observeSince(from)
    fun count(): Flow<Int> = dao.observeCount()

    suspend fun range(from: Long, to: Long): List<BpRecord> = dao.rangeAsc(from, to)
    suspend fun getAll(): List<BpRecord> = dao.getAllAsc()

    /** Raw rows including tombstones — for sync only. */
    suspend fun getAllRaw(): List<BpRecord> = dao.getAllRaw()

    /** Insert a new record, assigning a sync uid and updatedAt. */
    suspend fun add(record: BpRecord): Long {
        val now = System.currentTimeMillis()
        return dao.insert(
            record.copy(
                uid = record.uid.ifBlank { UUID.randomUUID().toString() },
                updatedAt = now,
                deleted = false
            )
        )
    }

    /** Update an existing record, preserving its uid and bumping updatedAt. */
    suspend fun update(record: BpRecord) {
        val now = System.currentTimeMillis()
        val uid = record.uid.ifBlank { dao.findById(record.id)?.uid ?: UUID.randomUUID().toString() }
        dao.update(record.copy(uid = uid, updatedAt = now, deleted = false))
    }

    /** Soft delete (tombstone) so the removal can sync to other devices. */
    suspend fun delete(record: BpRecord) {
        dao.softDelete(record.id, System.currentTimeMillis())
    }

    /** Hard-wipe local rows (used by "清除所有数据"). */
    suspend fun clear() = dao.clear()

    /**
     * Apply a merged record set to the local DB, keyed by uid:
     * insert unseen uids, and overwrite local rows only when the incoming
     * record is strictly newer (preserving the local primary key).
     */
    suspend fun mergeApply(records: List<BpRecord>) {
        for (r in records) {
            val existing = dao.findByUid(r.uid)
            if (existing == null) {
                dao.insert(r.copy(id = 0))
            } else if (r.updatedAt > existing.updatedAt) {
                dao.update(r.copy(id = existing.id))
            }
        }
    }
}
