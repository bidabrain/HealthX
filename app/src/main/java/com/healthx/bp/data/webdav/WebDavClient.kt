package com.healthx.bp.data.webdav

import com.healthx.bp.data.prefs.WebDavConfig
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

data class RemoteFile(val content: String?, val etag: String?) {
    val exists: Boolean get() = content != null
}

enum class PutResult { SUCCESS, CONFLICT, FAILED }

/**
 * WebDAV client for the sync engine. Primitives: PROPFIND (connectivity/list),
 * MKCOL (ensure dir), GET-with-ETag, conditional PUT (If-Match / If-None-Match),
 * DELETE. Works with Nextcloud, ownCloud, Synology, Jianguoyun (坚果云), etc.
 * Methods throw on hard errors (401/5xx); callers wrap in runCatching.
 */
class WebDavClient(private val config: WebDavConfig) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val credential = Credentials.basic(config.username, config.password)
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun baseUrl(): String {
        var raw = config.url.trim().trimEnd('/')
        if (raw.isEmpty()) throw IllegalArgumentException("服务器地址不能为空")
        if (!raw.startsWith("http://") && !raw.startsWith("https://")) raw = "https://$raw"
        val port = config.port.trim()
        if (port.isNotEmpty()) {
            val url = raw.toHttpUrlOrNull() ?: throw IllegalArgumentException("服务器地址无效")
            raw = url.newBuilder().port(port.toInt()).build().toString().trimEnd('/')
        }
        return raw
    }

    private fun dirUrl(): String = baseUrl() + "/" + config.remotePath.trim().trim('/') + "/"
    private fun fileUrl(name: String): String = dirUrl() + name
    private fun historyDirUrl(): String = dirUrl() + "history/"
    private fun historyFileUrl(name: String): String = historyDirUrl() + name

    private fun req(url: String) = Request.Builder().url(url).header("Authorization", credential)

    /** PROPFIND on the base URL — connectivity/credential check. */
    fun testConnection(): Result<Unit> = runCatching {
        val request = req(baseUrl() + "/").header("Depth", "0").method("PROPFIND", null).build()
        client.newCall(request).execute().use { resp ->
            if (resp.code == 401) error("认证失败：用户名或密码错误")
            if (!resp.isSuccessful && resp.code != 207) error("连接失败：HTTP ${resp.code}")
        }
    }

    private fun ensureDir(url: String) {
        client.newCall(req(url).method("MKCOL", null).build()).execute().use { resp ->
            // 201 created, 405 already exists, 301 redirect on existing — all fine.
            if (resp.code == 401) error("认证失败：用户名或密码错误")
        }
    }

    fun ensureSyncDir() = ensureDir(dirUrl())

    /** GET the sync file with its ETag. Returns exists=false on 404. */
    fun getWithEtag(name: String): RemoteFile {
        client.newCall(req(fileUrl(name)).get().build()).execute().use { resp ->
            if (resp.code == 404) return RemoteFile(null, null)
            if (resp.code == 401) error("认证失败：用户名或密码错误")
            if (!resp.isSuccessful) error("下载失败：HTTP ${resp.code}")
            return RemoteFile(resp.body?.string() ?: "", resp.header("ETag"))
        }
    }

    /**
     * Conditional PUT. When [etag] is non-null uses If-Match to avoid clobbering
     * a newer server copy; when null uses If-None-Match:* so a concurrently
     * created file yields 412 rather than being overwritten.
     */
    fun putConditional(name: String, content: String, etag: String?): PutResult {
        ensureSyncDir()
        val builder = req(fileUrl(name)).put(content.toRequestBody(jsonMedia))
        if (etag != null) builder.header("If-Match", etag) else builder.header("If-None-Match", "*")
        client.newCall(builder.build()).execute().use { resp ->
            return when (resp.code) {
                200, 201, 204 -> PutResult.SUCCESS
                412, 428 -> PutResult.CONFLICT
                401 -> error("认证失败：用户名或密码错误")
                else -> PutResult.FAILED
            }
        }
    }

    /** Best-effort: save [content] as a timestamped history snapshot, keep newest [keep]. */
    fun snapshotAndPrune(now: Long, content: String, keep: Int = 10) {
        runCatching {
            ensureDir(historyDirUrl())
            client.newCall(
                req(historyFileUrl("healthx-sync-$now.json")).put(content.toRequestBody(jsonMedia)).build()
            ).execute().close()

            val names = propfindNames(historyDirUrl())
                .filter { Regex("^healthx-sync-\\d+\\.json$").matches(it) }
                .sortedByDescending { it.removePrefix("healthx-sync-").removeSuffix(".json").toLongOrNull() ?: 0L }
            names.drop(keep).forEach { old ->
                runCatching {
                    client.newCall(req(historyFileUrl(old)).delete().build()).execute().close()
                }
            }
        }
    }

    /** History snapshot file names (newest-parseable), for the rollback screen. */
    fun listHistoryNames(): List<String> =
        propfindNames(historyDirUrl()).filter { Regex("^healthx-sync-\\d+\\.json$").matches(it) }

    /** Download a specific history snapshot's content. */
    fun downloadHistory(name: String): String {
        client.newCall(req(historyFileUrl(name)).get().build()).execute().use { resp ->
            if (resp.code == 404) error("快照不存在或已被清理")
            if (resp.code == 401) error("认证失败：用户名或密码错误")
            if (!resp.isSuccessful) error("下载失败：HTTP ${resp.code}")
            return resp.body?.string() ?: ""
        }
    }

    /** PROPFIND (Depth:1) and return the child file names under [dirUrl]. */
    private fun propfindNames(dirUrl: String): List<String> {
        val request = req(dirUrl).header("Depth", "1").method("PROPFIND", null).build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful && resp.code != 207) return emptyList()
            val body = resp.body?.string() ?: return emptyList()
            return Regex("<[^>]*?href>\\s*([^<]+?)\\s*</[^>]*?href>", RegexOption.IGNORE_CASE)
                .findAll(body)
                .map { it.groupValues[1] }
                .map { href -> URLDecoder.decode(href.trimEnd('/'), "UTF-8").substringAfterLast('/') }
                .filter { it.isNotBlank() }
                .toList()
        }
    }
}
