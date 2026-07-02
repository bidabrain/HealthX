package com.healthx.bp.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthx.bp.HealthXApp
import com.healthx.bp.data.db.BpRecord
import com.healthx.bp.domain.BpStatus
import com.healthx.bp.ui.components.BpLineChart
import com.healthx.bp.ui.components.ChartLegend
import com.healthx.bp.ui.components.SectionCard
import com.healthx.bp.ui.components.StatusDot
import com.healthx.bp.ui.components.TimeRangeTabs
import com.healthx.bp.util.Format
import com.healthx.bp.util.appViewModel

@Composable
fun HistoryScreen(onAdd: () -> Unit, onExport: () -> Unit, onEdit: (Long) -> Unit) {
    val app = LocalContext.current.applicationContext as HealthXApp
    val vm: HistoryViewModel = viewModel(factory = appViewModel { HistoryViewModel(app.graph.bpRepository) })
    val range by vm.range.collectAsStateWithLifecycle()
    val records by vm.recordsAsc.collectAsStateWithLifecycle()
    val desc = remember(records) { records.sortedByDescending { it.timestamp } }

    var menuFor by remember { mutableStateOf<Long?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("历史记录", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onExport) { Icon(Icons.Filled.Image, "导出图片") }
        }

        TimeRangeTabs(selected = range, onSelect = { vm.setRange(it) })
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionCard {
                    Text("血压趋势", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    ChartLegend()
                    BpLineChart(records = records, height = 200.dp)
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
                    Text("日期时间", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text("血压(mmHg)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(84.dp))
                    Text("心率", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(36.dp))
                }
            }
            if (desc.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("该时间段暂无记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(desc, key = { it.id }) { r ->
                    SectionCard(padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                        Box {
                            HistoryRow(r) { menuFor = r.id }
                            DropdownMenu(expanded = menuFor == r.id, onDismissRequest = { menuFor = null }) {
                                DropdownMenuItem(text = { Text("编辑") }, onClick = { menuFor = null; onEdit(r.id) })
                                DropdownMenuItem(text = { Text("删除") }, onClick = { menuFor = null; vm.delete(r) })
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun HistoryRow(r: BpRecord, onLongPress: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(BpStatus.fromKey(r.status))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(Format.dateTime(r.timestamp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            if (r.note.isNotBlank()) {
                Text(r.note, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        Text("${r.systolic} / ${r.diastolic}", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(84.dp))
        Row(Modifier.width(52.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${r.heartRate}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            IconButton(onClick = onLongPress, modifier = Modifier.size(24.dp)) {
                Text("⋮", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
