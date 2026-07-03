package com.healthx.bp.data.webdav

import com.healthx.bp.data.db.BpRecord
import com.healthx.bp.data.prefs.WebDavConfig
import com.healthx.bp.data.repository.BpRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SyncPayload(
    val app: String = "HealthX",
    val schema: Int = 2,
    val updatedAt: Long,
    val records: List<BpRecord>
)

data class SyncResult(val activeCount: Int, val totalWithTombstones: Int)

/** A server-side rollback snapshot (from the history/ folder). */
data class Snapshot(val name: String, val timestamp: Long)

/** What restoring a snapshot would do, for the confirm dialog. */
data class SnapshotPreview(val liveInSnapshot: Int, val willRestore: Int)

/**
 * Two-way WebDAV sync via a merge-by-uid strategy (LWW-Element-Set):
 * download → union-merge with local → apply to local → snapshot remote →
 * conditional upload → retry on 412. See WEBDAV_SYNC_DESIGN.md.
 */
class SyncManager(private val repository: BpRepository) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    private val fileName = "healthx-sync.json"
    private val mutex = Mutex() // serialize concurrent sync triggers

    suspend fun testConnection(config: WebDavConfig): Result<Unit> = withContext(Dispatchers.IO) {
        WebDavClient(config).testConnection()
    }

    suspend fun sync(config: WebDavConfig, now: Long): Result<SyncResult> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val client = WebDavClient(config)
                repeat(3) {
                    val remote = client.getWithEtag(fileName)
                    val remoteRecords = remote.content
                        ?.takeIf { it.isNotBlank() }
                        ?.let { json.decodeFromString<SyncPayload>(it).records }
                        ?: emptyList()

                    val local = repository.getAllRaw()
                    val merged = merge(local, remoteRecords)

                    // Pull remote-side additions/updates/tombstones into local.
                    repository.mergeApply(merged)

                    // No-op sync: merge result already equals the remote → don't snapshot
                    // or upload (avoids redundant history snapshots and network writes).
                    if (sameContent(merged, remoteRecords)) {
                        return@runCatching SyncResult(
                            activeCount = merged.count { !it.deleted },
                            totalWithTombstones = merged.size
                        )
                    }

                    // Keep a rollback snapshot of what was on the server before we overwrite it.
                    if (remote.exists) client.snapshotAndPrune(now, remote.content!!)

                    val payload = SyncPayload(updatedAt = now, records = merged)
                    when (client.putConditional(fileName, json.encodeToString(payload), remote.etag)) {
                        PutResult.SUCCESS ->
                            return@runCatching SyncResult(
                                activeCount = merged.count { !it.deleted },
                                totalWithTombstones = merged.size
                            )
                        PutResult.CONFLICT -> Unit // remote changed under us — loop and re-merge
                        PutResult.FAILED -> error("同步上传失败，请稍后再试")
                    }
                }
                error("同步冲突，重试多次仍未成功，请稍后再试")
            }
        }
    }

    // ---- Rollback (history snapshots) ----

    suspend fun listSnapshots(config: WebDavConfig): Result<List<Snapshot>> = withContext(Dispatchers.IO) {
        runCatching {
            WebDavClient(config).listHistoryNames()
                .mapNotNull { name ->
                    name.removePrefix("healthx-sync-").removeSuffix(".json").toLongOrNull()
                        ?.let { Snapshot(name, it) }
                }
                .sortedByDescending { it.timestamp }
        }
    }

    private fun snapshotRecords(content: String): List<BpRecord> =
        content.takeIf { it.isNotBlank() }?.let { json.decodeFromString<SyncPayload>(it).records } ?: emptyList()

    /** How many records would be brought back if this snapshot is restored. */
    suspend fun previewSnapshot(config: WebDavConfig, name: String): Result<SnapshotPreview> =
        withContext(Dispatchers.IO) {
            runCatching {
                val snap = snapshotRecords(WebDavClient(config).downloadHistory(name))
                val snapshotLive = snap.filter { !it.deleted }.map { it.uid }.toSet()
                val currentLive = repository.getAllRaw().filter { !it.deleted }.map { it.uid }.toSet()
                SnapshotPreview(
                    liveInSnapshot = snapshotLive.size,
                    willRestore = (snapshotLive - currentLive).size
                )
            }
        }

    /**
     * Restore a snapshot: re-apply every record that was *live* at that time with
     * a fresh updatedAt so it wins over any newer tombstone (resurrects deletions
     * and reverts edits to the snapshot values). Purely additive — never deletes,
     * so records added after the snapshot are preserved. Then pushes to the server.
     */
    suspend fun restoreSnapshot(config: WebDavConfig, name: String, now: Long): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val snap = snapshotRecords(WebDavClient(config).downloadHistory(name))
                val snapshotLive = snap.filter { !it.deleted }.map { it.uid }.toSet()
                val currentLive = repository.getAllRaw().filter { !it.deleted }.map { it.uid }.toSet()
                val willRestore = (snapshotLive - currentLive).size

                val forced = snap.filter { !it.deleted }
                    .map { it.copy(id = 0, deleted = false, updatedAt = now) }
                repository.mergeApply(forced)

                sync(config, now + 1).getOrThrow()
                willRestore
            }
        }

    /** Content equality ignoring the local-only [BpRecord.id]. */
    private fun sameContent(a: List<BpRecord>, b: List<BpRecord>): Boolean =
        a.map { it.copy(id = 0) }.sortedBy { it.uid } == b.map { it.copy(id = 0) }.sortedBy { it.uid }

    /** Union by uid; for the same uid keep the larger updatedAt (tie → remote, already placed). */
    private fun merge(local: List<BpRecord>, remote: List<BpRecord>): List<BpRecord> {
        val byUid = LinkedHashMap<String, BpRecord>()
        for (r in remote) if (r.uid.isNotBlank()) byUid[r.uid] = r
        for (l in local) {
            if (l.uid.isBlank()) continue
            val existing = byUid[l.uid]
            if (existing == null || l.updatedAt > existing.updatedAt) byUid[l.uid] = l
        }
        return byUid.values.toList()
    }
}
