package com.healthx.bp.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthx.bp.data.db.BpRecord
import com.healthx.bp.data.repository.BpRepository
import com.healthx.bp.util.TimeRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(private val repository: BpRepository) : ViewModel() {

    private val _range = MutableStateFlow(TimeRange.D30)
    val range: StateFlow<TimeRange> = _range

    /** Ascending records within the selected range (for the chart). */
    val recordsAsc: StateFlow<List<BpRecord>> = _range
        .flatMapLatest { r -> repository.since(r.startMillis()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setRange(r: TimeRange) { _range.value = r }

    fun delete(record: BpRecord) {
        viewModelScope.launch { repository.delete(record) }
    }
}
