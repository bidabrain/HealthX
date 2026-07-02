package com.healthx.bp.ui.export

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthx.bp.data.repository.BpRepository
import com.healthx.bp.util.Format
import com.healthx.bp.util.ImageExporter
import com.healthx.bp.util.TimeRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ExportUiState(
    val range: TimeRange = TimeRange.D30,
    val includeChart: Boolean = true,
    val includeTable: Boolean = true,
    val generating: Boolean = false,
    val preview: Bitmap? = null,
    val error: String? = null
)

class ExportViewModel(private val repository: BpRepository) : ViewModel() {

    private val _state = MutableStateFlow(ExportUiState())
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

    fun setRange(r: TimeRange) = _state.update { it.copy(range = r, preview = null) }
    fun toggleChart(v: Boolean) = _state.update { it.copy(includeChart = v, preview = null) }
    fun toggleTable(v: Boolean) = _state.update { it.copy(includeTable = v, preview = null) }

    fun generate() {
        val s = _state.value
        if (!s.includeChart && !s.includeTable) {
            _state.update { it.copy(error = "请至少选择一项内容") }
            return
        }
        _state.update { it.copy(generating = true, error = null) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val from = s.range.startMillis(now)
            val records = repository.range(from, now)
            val rangeLabel = if (records.isEmpty()) s.range.label
            else "${Format.date(records.first().timestamp)} ~ ${Format.date(records.last().timestamp)}"
            val bmp = withContext(Dispatchers.Default) {
                ImageExporter.render(
                    recordsAsc = records,
                    options = ImageExporter.Options(
                        includeChart = s.includeChart,
                        includeTable = s.includeTable,
                        rangeLabel = rangeLabel
                    ),
                    generatedAt = now
                )
            }
            _state.update { it.copy(generating = false, preview = bmp) }
        }
    }

    fun clearPreview() = _state.update { it.copy(preview = null) }
}
