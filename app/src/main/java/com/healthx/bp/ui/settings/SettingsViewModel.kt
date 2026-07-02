package com.healthx.bp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthx.bp.data.prefs.AppSettings
import com.healthx.bp.data.prefs.SettingsRepository
import com.healthx.bp.data.repository.BpRepository
import com.healthx.bp.domain.BpStatus
import com.healthx.bp.util.Format
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: BpRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setThemeMode(mode: String) = viewModelScope.launch { settingsRepo.setThemeMode(mode) }

    /** Store a new 6-digit PIN (salted hash) and enable the lock. */
    fun setPin(pin: String) = viewModelScope.launch {
        val salt = com.healthx.bp.util.Pin.newSalt()
        val hash = com.healthx.bp.util.Pin.hash(pin, salt)
        settingsRepo.setPin(hash, salt)
    }

    /** Disable the lock and wipe the stored PIN. */
    fun disableLock() = viewModelScope.launch { settingsRepo.clearPin() }

    fun clearAll(onDone: () -> Unit) = viewModelScope.launch {
        repository.clear()
        onDone()
    }

    /** Build a CSV string of all records for export/sharing. */
    fun buildCsv(onReady: (String) -> Unit) = viewModelScope.launch {
        val rows = repository.getAll().sortedByDescending { it.timestamp }
        val sb = StringBuilder("日期时间,收缩压,舒张压,心率,状态,备注\n")
        rows.forEach { r ->
            val note = r.note.replace(",", " ").replace("\n", " ")
            sb.append("${Format.dateTime(r.timestamp)},${r.systolic},${r.diastolic},${r.heartRate},${BpStatus.fromKey(r.status).label},$note\n")
        }
        onReady(sb.toString())
    }
}
