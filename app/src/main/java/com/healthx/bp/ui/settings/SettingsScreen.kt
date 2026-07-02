package com.healthx.bp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthx.bp.HealthXApp
import com.healthx.bp.ui.components.SectionCard
import com.healthx.bp.ui.theme.BpHigh
import com.healthx.bp.util.Sharing
import com.healthx.bp.util.appViewModel

@Composable
fun SettingsScreen(onSync: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as HealthXApp
    val vm: SettingsViewModel = viewModel(
        factory = appViewModel { SettingsViewModel(app.graph.bpRepository, app.graph.settingsRepository) }
    )
    val settings by vm.settings.collectAsStateWithLifecycle()

    var showClear by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }

    // Full-screen PIN setup overlay.
    if (showPinSetup) {
        com.healthx.bp.ui.lock.PinSetup(
            onDone = { pin -> vm.setPin(pin); showPinSetup = false },
            onCancel = { showPinSetup = false }
        )
        return
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("设置", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        GroupLabel("数据管理")
        SectionCard(padding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)) {
            SettingRow(Icons.Filled.Sync, "WebDAV 同步", tint = MaterialTheme.colorScheme.primary, onClick = onSync)
            SettingRow(Icons.Filled.Description, "导出数据 (CSV)", tint = MaterialTheme.colorScheme.primary, onClick = {
                vm.buildCsv { csv ->
                    val uri = Sharing.cacheTextUri(context, "healthx-records.csv", csv)
                    Sharing.shareFile(context, uri, "text/csv", "导出血压数据")
                }
            })
            SettingRow(Icons.Filled.DeleteForever, "清除所有数据", tint = BpHigh, textColor = BpHigh, onClick = { showClear = true })
        }

        GroupLabel("通用设置")
        SectionCard(padding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)) {
            ValueRow("血压单位", "mmHg")
            ValueRow("心率单位", "次/分")
            ValueRow("主题模式", themeLabel(settings.themeMode), onClick = { showTheme = true })
            SwitchRow("密码保护", settings.lockActive) { checked ->
                if (checked) showPinSetup = true else vm.disableLock()
            }
            if (settings.lockActive) {
                ValueRow("修改 PIN 密码", "", onClick = { showPinSetup = true })
            }
        }

        GroupLabel("关于")
        SectionCard(padding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)) {
            SettingRow(null, "关于我们", onClick = {})
            ValueRow("版本", "1.0.0")
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("清除所有数据") },
            text = { Text("此操作将永久删除全部血压记录，且无法恢复。建议先备份。确定继续？") },
            confirmButton = {
                TextButton(onClick = { vm.clearAll { }; showClear = false }) {
                    Text("确定删除", color = BpHigh)
                }
            },
            dismissButton = { TextButton(onClick = { showClear = false }) { Text("取消") } }
        )
    }

    if (showTheme) {
        AlertDialog(
            onDismissRequest = { showTheme = false },
            title = { Text("主题模式") },
            text = {
                Column {
                    listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (key, label) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.setThemeMode(key); showTheme = false }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, modifier = Modifier.weight(1f))
                            if (settings.themeMode == key) Text("✓", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTheme = false }) { Text("关闭") } }
        )
    }
}

private fun themeLabel(mode: String) = when (mode) {
    "light" -> "浅色"; "dark" -> "深色"; else -> "跟随系统"
}

@Composable
private fun GroupLabel(text: String) {
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
}

@Composable
private fun SettingRow(
    icon: ImageVector?,
    title: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(title, fontSize = 15.sp, color = textColor, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ValueRow(title: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onClick != null) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
