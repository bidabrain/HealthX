package com.healthx.bp.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthx.bp.data.db.BpRecord
import com.healthx.bp.data.repository.BpRepository
import com.healthx.bp.domain.BpStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecordForm(
    val id: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val systolic: String = "",
    val diastolic: String = "",
    val heartRate: String = "",
    // Auto-determined from the values; null means data incomplete (shown gray).
    val status: BpStatus? = null,
    val note: String = "",
    val saved: Boolean = false,
    val error: String? = null
) {
    val isEdit: Boolean get() = id != null
    val canSave: Boolean
        get() = systolic.toIntOrNull() != null && diastolic.toIntOrNull() != null
}

class RecordViewModel(
    private val repository: BpRepository,
    private val recordId: Long?
) : ViewModel() {

    private val _form = MutableStateFlow(RecordForm())
    val form: StateFlow<RecordForm> = _form.asStateFlow()

    init {
        if (recordId != null) {
            viewModelScope.launch {
                repository.getAll().firstOrNull { it.id == recordId }?.let { r ->
                    _form.value = RecordForm(
                        id = r.id,
                        timestamp = r.timestamp,
                        systolic = r.systolic.toString(),
                        diastolic = r.diastolic.toString(),
                        heartRate = r.heartRate.toString(),
                        status = BpStatus.classify(r.systolic, r.diastolic),
                        note = r.note
                    )
                }
            }
        }
    }

    fun setTimestamp(ts: Long) = _form.update { it.copy(timestamp = ts) }

    fun setSystolic(v: String) = _form.update { autoStatus(it.copy(systolic = v.filterDigits(3))) }
    fun setDiastolic(v: String) = _form.update { autoStatus(it.copy(diastolic = v.filterDigits(3))) }
    fun setHeartRate(v: String) = _form.update { it.copy(heartRate = v.filterDigits(3)) }
    fun setNote(v: String) = _form.update { it.copy(note = v.take(100)) }

    private fun String.filterDigits(max: Int) = filter { it.isDigit() }.take(max)

    /** Recompute status purely from the values; null when either is missing. */
    private fun autoStatus(f: RecordForm): RecordForm {
        val s = f.systolic.toIntOrNull()
        val d = f.diastolic.toIntOrNull()
        return f.copy(status = if (s != null && d != null) BpStatus.classify(s, d) else null)
    }

    fun save() {
        val f = _form.value
        val s = f.systolic.toIntOrNull()
        val d = f.diastolic.toIntOrNull()
        if (s == null || d == null) {
            _form.update { it.copy(error = "请输入有效的收缩压和舒张压") }
            return
        }
        val record = BpRecord(
            id = f.id ?: 0,
            timestamp = f.timestamp,
            systolic = s,
            diastolic = d,
            heartRate = f.heartRate.toIntOrNull() ?: 0,
            status = BpStatus.classify(s, d).key,
            note = f.note.trim()
        )
        viewModelScope.launch {
            if (f.isEdit) repository.update(record) else repository.add(record)
            _form.update { it.copy(saved = true) }
        }
    }
}
