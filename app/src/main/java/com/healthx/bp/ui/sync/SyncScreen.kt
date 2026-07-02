package com.healthx.bp.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthx.bp.HealthXApp
import com.healthx.bp.ui.backup.DetailTopBar
import com.healthx.bp.ui.backup.LabeledField
import com.healthx.bp.ui.theme.BpHigh
import com.healthx.bp.ui.theme.BpNormal
import com.healthx.bp.util.Format
import com.healthx.bp.util.appViewModel

@Composable
fun SyncScreen(onBack: () -> Unit, onHistory: () -> Unit) {
    val app = LocalContext.current.applicationContext as HealthXApp
    val vm: SyncViewModel = viewModel(
        factory = appViewModel { SyncViewModel(app.graph.settingsRepository, app.graph.syncManager) }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val cfg = state.config

    Column(Modifier.fillMaxWidth()) {
        DetailTopBar("WebDAV 同步", onBack)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("启用 WebDAV 同步", fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Switch(checked = cfg.enabled, onCheckedChange = { vm.setEnabled(it) })
            }

            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(12.dp)
            ) {
                Text(
                    "开启后，App 切换前台/后台会自动双向同步。多设备按记录合并，不会互相覆盖；删除会同步，且每次上传前在服务器保留历史快照可回滚。",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(6.dp))

            LabeledField("服务器地址", cfg.url, { v -> vm.update { it.copy(url = v) } }, "https://dav.example.com/webdav")
            LabeledField("用户名", cfg.username, { v -> vm.update { it.copy(username = v) } }, "username")
            LabeledField("密码", cfg.password, { v -> vm.update { it.copy(password = v) } }, isPassword = true)
            LabeledField("端口（可选）", cfg.port, { v -> vm.update { it.copy(port = v.filter { c -> c.isDigit() }) } }, "443", KeyboardType.Number)
            LabeledField("远程路径（可选）", cfg.remotePath, { v -> vm.update { it.copy(remotePath = v) } }, "/healthx")

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { vm.syncNow(System.currentTimeMillis()) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (state.busy) CircularProgressIndicator(Modifier.height(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("立即同步", fontSize = 15.sp)
            }

            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onHistory() }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("同步历史 / 回滚", fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text("查看快照 ›", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(4.dp))
            if (state.lastSyncAt > 0) {
                Text("上次同步：${Format.dateTime(state.lastSyncAt)}",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.message?.let { msg ->
                Text(msg, fontSize = 13.sp, color = if (state.isError) BpHigh else BpNormal,
                    modifier = Modifier.padding(top = 6.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
