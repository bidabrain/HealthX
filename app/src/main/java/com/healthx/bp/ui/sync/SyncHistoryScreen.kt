package com.healthx.bp.ui.sync

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthx.bp.HealthXApp
import com.healthx.bp.ui.backup.DetailTopBar
import com.healthx.bp.ui.components.SectionCard
import com.healthx.bp.util.Format
import com.healthx.bp.util.appViewModel

@Composable
fun SyncHistoryScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as HealthXApp
    val vm: SyncHistoryViewModel = viewModel(
        factory = appViewModel { SyncHistoryViewModel(app.graph.settingsRepository, app.graph.syncManager) }
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        DetailTopBar("同步历史", onBack)

        Text(
            "每次同步前的服务器快照（最近 10 份）。恢复只会找回被删/缺失的记录，不会删除此后新增的记录。",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        state.result?.let { msg ->
            Text(
                msg, fontSize = 13.sp,
                color = if (state.resultError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            state.snapshots.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无历史快照（同步一次后才会生成）", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.snapshots, key = { it.name }) { snap ->
                    SectionCard(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.select(snap) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.History, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(0.dp))
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(Format.dateTime(snap.timestamp), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("点击查看并可恢复", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }

    // Restore confirm dialog
    val selected = state.selected
    if (selected != null) {
        AlertDialog(
            onDismissRequest = { if (!state.restoring) vm.dismissDialog() },
            title = { Text("恢复到该版本") },
            text = {
                Column {
                    Text("时间：${Format.dateTime(selected.timestamp)}", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    when {
                        state.previewing -> Text("正在读取快照…", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        state.preview != null -> {
                            val p = state.preview!!
                            Text("该版本含 ${p.liveInSnapshot} 条记录。", fontSize = 13.sp)
                            Text(
                                if (p.willRestore > 0) "恢复后将找回 ${p.willRestore} 条当前已删除/缺失的记录，且不会删除此后新增的记录。"
                                else "当前记录已包含该版本全部内容，恢复不会有变化。",
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { vm.confirmRestore(System.currentTimeMillis()) },
                    enabled = !state.restoring && !state.previewing
                ) { Text(if (state.restoring) "恢复中…" else "确定恢复") }
            },
            dismissButton = { TextButton(onClick = { vm.dismissDialog() }, enabled = !state.restoring) { Text("取消") } }
        )
    }
}
