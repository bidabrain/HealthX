package com.healthx.bp.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthx.bp.data.db.BpRecord
import com.healthx.bp.data.repository.BpRepository
import com.healthx.bp.domain.BpStats
import com.healthx.bp.util.TimeRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class StatsUiState(
    val records: List<BpRecord> = emptyList(),
    val stats: BpStats = BpStats.EMPTY
)

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(repository: BpRepository) : ViewModel() {

    private val _range = MutableStateFlow(TimeRange.D30)
    val range: StateFlow<TimeRange> = _range

    val state: StateFlow<StatsUiState> = _range
        .flatMapLatest { r -> repository.since(r.startMillis()) }
        .map { records -> StatsUiState(records = records, stats = BpStats.from(records)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    fun setRange(r: TimeRange) { _range.value = r }
}
