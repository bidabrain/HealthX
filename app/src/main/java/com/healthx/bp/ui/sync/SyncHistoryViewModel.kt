package com.healthx.bp.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthx.bp.data.prefs.SettingsRepository
import com.healthx.bp.data.prefs.WebDavConfig
import com.healthx.bp.data.webdav.Snapshot
import com.healthx.bp.data.webdav.SnapshotPreview
import com.healthx.bp.data.webdav.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SyncHistoryState(
    val loading: Boolean = true,
    val snapshots: List<Snapshot> = emptyList(),
    val error: String? = null,
    // confirm dialog
    val selected: Snapshot? = null,
    val preview: SnapshotPreview? = null,
    val previewing: Boolean = false,
    val restoring: Boolean = false,
    val result: String? = null,
    val resultError: Boolean = false
)

class SyncHistoryViewModel(
    private val settingsRepo: SettingsRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(SyncHistoryState())
    val state: StateFlow<SyncHistoryState> = _state.asStateFlow()

    private var config: WebDavConfig = WebDavConfig()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            config = settingsRepo.settings.first().webDav
            syncManager.listSnapshots(config)
                .onSuccess { list -> _state.update { it.copy(loading = false, snapshots = list) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message ?: "读取失败") } }
        }
    }

    /** Tapping a snapshot: download it and show a restore confirm dialog with a preview. */
    fun select(snapshot: Snapshot) {
        _state.update { it.copy(selected = snapshot, preview = null, previewing = true) }
        viewModelScope.launch {
            syncManager.previewSnapshot(config, snapshot.name)
                .onSuccess { p -> _state.update { it.copy(preview = p, previewing = false) } }
                .onFailure { e -> _state.update { it.copy(selected = null, previewing = false, result = e.message, resultError = true) } }
        }
    }

    fun dismissDialog() = _state.update { it.copy(selected = null, preview = null, previewing = false) }

    fun confirmRestore(now: Long) {
        val snap = _state.value.selected ?: return
        _state.update { it.copy(restoring = true) }
        viewModelScope.launch {
            syncManager.restoreSnapshot(config, snap.name, now)
                .onSuccess { count ->
                    _state.update {
                        it.copy(
                            restoring = false, selected = null, preview = null,
                            result = "已恢复，共找回 $count 条记录", resultError = false
                        )
                    }
                    refresh() // reload the snapshot list (restore pushes a new snapshot)
                }
                .onFailure { e ->
                    _state.update { it.copy(restoring = false, selected = null, preview = null, result = e.message ?: "恢复失败", resultError = true) }
                }
        }
    }

    fun clearResult() = _state.update { it.copy(result = null) }
}
