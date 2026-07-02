package com.healthx.bp.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthx.bp.HealthXApp
import com.healthx.bp.domain.BpStats
import com.healthx.bp.domain.BpStatus
import com.healthx.bp.ui.components.BpLineChart
import com.healthx.bp.ui.components.ChartLegend
import com.healthx.bp.ui.components.DonutChart
import com.healthx.bp.ui.components.SectionCard
import com.healthx.bp.ui.components.SectionTitle
import com.healthx.bp.ui.components.TimeRangeTabs
import com.healthx.bp.ui.theme.BpDiastolic
import com.healthx.bp.ui.theme.BpHeart
import com.healthx.bp.ui.theme.BpSystolic
import com.healthx.bp.util.appViewModel
import kotlin.math.roundToInt

@Composable
fun StatsScreen() {
    val app = LocalContext.current.applicationContext as HealthXApp
    val vm: StatsViewModel = viewModel(factory = appViewModel { StatsViewModel(app.graph.bpRepository) })
    val range by vm.range.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val stats = state.stats

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("统计", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        TimeRangeTabs(selected = range, onSelect = { vm.setRange(it) })

        // Averages
        SectionCard {
            SectionTitle("平均值")
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AvgTile("平均收缩压", if (stats.count == 0) "--" else "${stats.avgSystolic}", "mmHg", BpSystolic, Modifier.weight(1f))
                AvgTile("平均舒张压", if (stats.count == 0) "--" else "${stats.avgDiastolic}", "mmHg", BpDiastolic, Modifier.weight(1f))
                AvgTile("平均心率", if (stats.count == 0) "--" else "${stats.avgHeart}", "次/分", BpHeart, Modifier.weight(1f))
            }
        }

        // Distribution
        SectionCard {
            SectionTitle("血压分布")
            Spacer(Modifier.height(12.dp))
            if (stats.count == 0) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(120.dp)) {
                        DonutChart(
                            slices = BpStatus.entries.map { it.color to (stats.distribution[it] ?: 0).toFloat() },
                            modifier = Modifier.size(120.dp)
                        )
                    }
                    Spacer(Modifier.width(20.dp))
                    Column(Modifier.weight(1f)) {
                        BpStatus.entries.forEach { s -> DistRow(s, stats) }
                    }
                }
            }
        }

        // Trend (systolic + diastolic)
        SectionCard {
            SectionTitle("血压趋势")
            Spacer(Modifier.height(4.dp))
            ChartLegend(showHeart = false)
            BpLineChart(records = state.records, height = 180.dp, showHeart = false)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AvgTile(label: String, value: String, unit: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(
        modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(12.dp)
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(unit, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DistRow(status: BpStatus, stats: BpStats) {
    val pct = (stats.ratio(status) * 100).roundToInt()
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(status.color))
        Spacer(Modifier.width(8.dp))
        Text(status.label, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text("$pct%", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
