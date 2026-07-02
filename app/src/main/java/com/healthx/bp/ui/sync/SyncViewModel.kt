package com.healthx.bp.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthx.bp.data.prefs.SettingsRepository
import com.healthx.bp.data.prefs.WebDavConfig
import com.healthx.bp.data.webdav.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SyncUiState(
    val config: WebDavConfig = WebDavConfig(),
    val loaded: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val lastSyncAt: Long = 0L
)

class SyncViewModel(
    private val settingsRepo: SettingsRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(SyncUiState())
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val s = settingsRepo.settings.first()
            _state.update { it.copy(config = s.webDav, lastSyncAt = s.webDav.lastSyncAt, loaded = true) }
        }
    }

    fun update(transform: (WebDavConfig) -> WebDavConfig) {
        _state.update { it.copy(config = transform(it.config), message = null) }
    }

    fun setEnabled(enabled: Boolean) {
        _state.update { it.copy(config = it.config.copy(enabled = enabled)) }
        viewModelScope.launch { settingsRepo.updateWebDav(_state.value.config) }
    }

    /** Save config and run a two-way sync immediately. */
    fun syncNow(now: Long) {
        val cfg = _state.value.config
        if (cfg.url.isBlank()) {
            _state.update { it.copy(message = "请填写服务器地址", isError = true) }
            return
        }
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            settingsRepo.updateWebDav(cfg.copy(enabled = true))
            syncManager.sync(cfg, now)
                .onSuccess { result ->
                    settingsRepo.setLastSyncAt(now)
                    _state.update {
                        it.copy(
                            busy = false,
                            isError = false,
                            lastSyncAt = now,
                            message = "同步成功，本地共 ${result.activeCount} 条记录"
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(busy = false, isError = true, message = e.message ?: "同步失败") }
                }
        }
    }
}
