package com.healthx.bp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthx.bp.data.db.BpRecord
import com.healthx.bp.data.repository.BpRepository
import com.healthx.bp.util.TimeRange
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val latest: BpRecord? = null,
    val recent: List<BpRecord> = emptyList(),
    val weekCount: Int = 0,
    val weekAvgSys: Int = 0,
    val weekAvgDia: Int = 0,
    val weekAvgHeart: Int = 0
)

class HomeViewModel(repository: BpRepository) : ViewModel() {

    val state: StateFlow<HomeUiState> = repository.all()
        .map { all ->
            val weekStart = TimeRange.D7.startMillis()
            val week = all.filter { it.timestamp >= weekStart }
            val n = week.size
            HomeUiState(
                latest = all.firstOrNull(),
                recent = all.take(4),
                weekCount = n,
                weekAvgSys = if (n == 0) 0 else week.sumOf { it.systolic } / n,
                weekAvgDia = if (n == 0) 0 else week.sumOf { it.diastolic } / n,
                weekAvgHeart = if (n == 0) 0 else week.sumOf { it.heartRate } / n
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}
