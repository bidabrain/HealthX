package com.healthx.bp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.healthx.bp.data.db.BpRecord
import com.healthx.bp.data.prefs.AppSettings
import com.healthx.bp.domain.BpStatus
import com.healthx.bp.ui.components.SectionCard
import com.healthx.bp.ui.components.SectionTitle
import com.healthx.bp.ui.components.StatusChip
import com.healthx.bp.ui.components.StatusDot
import com.healthx.bp.ui.theme.BpDiastolic
import com.healthx.bp.ui.theme.BpSystolic
import com.healthx.bp.util.Format
import com.healthx.bp.util.appViewModel

@Composable
fun HomeScreen(onAddRecord: () -> Unit, onSeeAll: () -> Unit, onOpenSync: () -> Unit) {
    val app = LocalContext.current.applicationContext as HealthXApp
    val vm: HomeViewModel = viewModel(factory = appViewModel { HomeViewModel(app.graph.bpRepository) })
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by app.graph.settingsRepository.settings.collectAsStateWithLifecycle(AppSettings())

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("首页", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            SyncStatusChip(settings.webDav.enabled, settings.webDav.lastSyncAt, onOpenSync)
        }

        LatestCard(state.latest, onAddRecord)
        WeekOverviewCard(state, onSeeAll)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LatestCard(latest: BpRecord?, onAddRecord: () -> Unit) {
    SectionCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionTitle("最新血压")
            Text(
                latest?.let { Format.dateTime(it.timestamp) } ?: "暂无记录",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(14.dp))
        if (latest == null) {
            Text("还没有血压记录，点击下方按钮开始记录。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${latest.systolic}", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = BpSystolic)
                Text(" / ", fontSize = 34.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${latest.diastolic}", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = BpDiastolic)
                Spacer(Modifier.width(6.dp))
                Text("mmHg", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
            }
            Row {
                Text("收缩压", color = BpSystolic, fontSize = 12.sp, modifier = Modifier.width(64.dp))
                Text("舒张压", color = BpDiastolic, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text("心率 ${latest.heartRate} 次/分", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("状态：", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                StatusChip(BpStatus.fromKey(latest?.status))
            }
            Button(
                onClick = onAddRecord,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.width(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("记录血压", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun WeekOverviewCard(state: HomeUiState, onSeeAll: () -> Unit) {
    SectionCard {
        SectionTitle("本周概览")
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("平均血压", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (state.weekCount == 0) "-- / --" else "${state.weekAvgSys} / ${state.weekAvgDia}",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
                Text("mmHg", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) {
                Text("平均心率", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (state.weekCount == 0) "--" else "${state.weekAvgHeart}",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
                Text("次/分", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("最近记录", fontWeight = FontWeight.SemiBold)
            Text("查看全部", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(2.dp).width(64.dp))
        }
        Spacer(Modifier.height(6.dp))
        if (state.recent.isEmpty()) {
            Text("暂无记录", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            state.recent.forEach { r -> RecentRow(r) }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "查看全部历史",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .clickable { onSeeAll() }
        )
    }
}

@Composable
private fun SyncStatusChip(enabled: Boolean, lastSyncAt: Long, onClick: () -> Unit) {
    val text = when {
        !enabled -> "未开启同步"
        lastSyncAt <= 0L -> "待同步"
        else -> "已同步 ${Format.monthDay(lastSyncAt)} ${Format.time(lastSyncAt)}"
    }
    val color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.10f))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Sync, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 11.sp, color = color)
    }
}

@Composable
private fun RecentRow(r: BpRecord) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(BpStatus.fromKey(r.status))
        Spacer(Modifier.width(8.dp))
        Text(Format.dateTime(r.timestamp), fontSize = 13.sp, modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface)
        Text("${r.systolic} / ${r.diastolic}", fontSize = 13.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.width(72.dp))
        Text("${r.heartRate}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp))
    }
}
