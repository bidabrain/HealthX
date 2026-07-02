package com.healthx.bp.ui.export

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthx.bp.HealthXApp
import com.healthx.bp.ui.backup.DetailTopBar
import com.healthx.bp.ui.components.SectionCard
import com.healthx.bp.ui.components.TimeRangeTabs
import com.healthx.bp.util.Sharing
import com.healthx.bp.util.appViewModel

@Composable
fun ExportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as HealthXApp
    val vm: ExportViewModel = viewModel(factory = appViewModel { ExportViewModel(app.graph.bpRepository) })
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(if (state.preview != null) "图片预览" else "导出图片", onBack)

        val preview = state.preview
        if (preview == null) {
            // ---- Configuration ----
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SectionCard {
                    Text("选择内容", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    CheckRow("血压趋势图", state.includeChart) { vm.toggleChart(it) }
                    CheckRow("历史记录列表", state.includeTable) { vm.toggleTable(it) }
                }
                SectionCard {
                    Text("时间范围", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    TimeRangeTabs(selected = state.range, onSelect = { vm.setRange(it) })
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                Button(
                    onClick = { vm.generate() },
                    enabled = !state.generating,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (state.generating) CircularProgressIndicator(Modifier.height(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("生成图片预览", fontSize = 15.sp)
                }
            }
        } else {
            // ---- Preview + actions ----
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)
                ) {
                    Image(
                        bitmap = preview.asImageBitmap(),
                        contentDescription = "血压历史图片",
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("重新选择", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth()
                            .clickable { vm.clearPreview() }
                            .padding(4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val ok = Sharing.saveBitmapToGallery(context, preview, "HealthX_${System.currentTimeMillis()}.jpg")
                            Toast.makeText(context, if (ok) "已保存到相册" else "保存失败", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.Download, null, modifier = Modifier.height(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("保存到相册")
                    }
                    Button(
                        onClick = {
                            val uri = Sharing.cacheBitmapUri(context, "HealthX_share.jpg", preview)
                            Sharing.shareFile(context, uri, "image/jpeg", "分享血压记录")
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Share, null, modifier = Modifier.height(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("分享")
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 15.sp)
    }
}
