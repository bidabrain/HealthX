package com.healthx.bp.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthx.bp.HealthXApp
import com.healthx.bp.domain.BpStatus
import com.healthx.bp.ui.components.SectionCard
import com.healthx.bp.ui.theme.BpDiastolic
import com.healthx.bp.ui.theme.BpSystolic
import com.healthx.bp.util.Format
import com.healthx.bp.util.appViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(recordId: Long?, onDone: () -> Unit) {
    val app = LocalContext.current.applicationContext as HealthXApp
    val vm: RecordViewModel = viewModel(
        factory = appViewModel { RecordViewModel(app.graph.bpRepository, recordId) }
    )
    val form by vm.form.collectAsStateWithLifecycle()

    LaunchedEffect(form.saved) { if (form.saved) onDone() }

    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
            Text(
                if (form.isEdit) "编辑记录" else "记录血压",
                fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center
            )
            IconButton(onClick = { vm.save() }, enabled = form.canSave) {
                Icon(Icons.Filled.Check, "保存",
                    tint = if (form.canSave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard {
                FieldLabel("日期时间")
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .clickable { showDate = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(Format.dateTime(form.timestamp), fontSize = 15.sp)
                    Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(16.dp))
                MetricField("收缩压 (高压)", form.systolic, "mmHg", BpSystolic) { vm.setSystolic(it) }
                Spacer(Modifier.height(12.dp))
                MetricField("舒张压 (低压)", form.diastolic, "mmHg", BpDiastolic) { vm.setDiastolic(it) }
                Spacer(Modifier.height(12.dp))
                MetricField("心率", form.heartRate, "次/分", MaterialTheme.colorScheme.onSurface) { vm.setHeartRate(it) }
            }

            SectionCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        FieldLabel("状态")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "根据血压数值自动判定",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AutoStatusChip(form.status)
                }
            }

            SectionCard {
                FieldLabel("备注")
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = form.note,
                    onValueChange = { vm.setNote(it) },
                    placeholder = { Text("输入备注（可选）", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Text("${form.note.length}/100", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.End)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // Date picker
    if (showDate) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = form.timestamp)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { picked ->
                        // keep existing time, replace date
                        val cal = Calendar.getInstance().apply { timeInMillis = form.timestamp }
                        val p = Calendar.getInstance().apply { timeInMillis = picked }
                        cal.set(p.get(Calendar.YEAR), p.get(Calendar.MONTH), p.get(Calendar.DAY_OF_MONTH))
                        vm.setTimestamp(cal.timeInMillis)
                    }
                    showDate = false
                    showTime = true
                }) { Text("下一步") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("取消") } }
        ) { DatePicker(state = dateState) }
    }

    // Time picker
    if (showTime) {
        val cal = Calendar.getInstance().apply { timeInMillis = form.timestamp }
        val timeState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        DatePickerDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    val c = Calendar.getInstance().apply { timeInMillis = form.timestamp }
                    c.set(Calendar.HOUR_OF_DAY, timeState.hour)
                    c.set(Calendar.MINUTE, timeState.minute)
                    c.set(Calendar.SECOND, 0)
                    vm.setTimestamp(c.timeInMillis)
                    showTime = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("取消") } }
        ) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                TimePicker(state = timeState)
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun MetricField(label: String, value: String, unit: String, accent: Color, onChange: (String) -> Unit) {
    Column {
        Text(label, fontSize = 14.sp, color = accent, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(10.dp),
                placeholder = { Text("--", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
            Spacer(Modifier.width(10.dp))
            Text(unit, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(44.dp))
        }
    }
}

@Composable
private fun AutoStatusChip(status: BpStatus?) {
    val gray = MaterialTheme.colorScheme.onSurfaceVariant
    val color = status?.color ?: gray
    val label = status?.label ?: "数据不全"
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
