package com.healthx.bp

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.healthx.bp.data.db.BpDatabase
import com.healthx.bp.data.prefs.SettingsRepository
import com.healthx.bp.data.repository.BpRepository
import com.healthx.bp.data.webdav.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * In-memory lock flag. Starts locked; resets to locked whenever the app goes to
 * the background, so returning to the app (or a cold start) requires the PIN
 * again. Whether the lock is actually shown is gated by [com.healthx.bp.data.prefs.AppSettings.lockActive].
 */
class AppLock {
    private val _locked = MutableStateFlow(true)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()
    fun unlock() { _locked.value = false }
    fun lock() { _locked.value = true }
}

/** Application-scoped, hand-rolled dependency graph (no Hilt needed for this size). */
class HealthXApp : Application() {
    lateinit var graph: AppGraph
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                triggerAutoSync()          // pull latest when returning to foreground
            }
            override fun onStop(owner: LifecycleOwner) {
                graph.appLock.lock()       // re-lock on background
                triggerAutoSync()          // push latest when leaving
            }
        })
    }

    /** Fire-and-forget background sync if WebDAV sync is enabled. */
    fun triggerAutoSync() {
        appScope.launch {
            val settings = graph.settingsRepository.settings.first()
            if (settings.webDav.enabled) {
                graph.syncManager.sync(settings.webDav, System.currentTimeMillis())
                    .onSuccess { graph.settingsRepository.setLastSyncAt(System.currentTimeMillis()) }
            }
        }
    }
}

class AppGraph(app: Application) {
    private val db = BpDatabase.get(app)
    val bpRepository = BpRepository(db.bpDao())
    val settingsRepository = SettingsRepository(app)
    val syncManager = SyncManager(bpRepository)
    val appLock = AppLock()
}
