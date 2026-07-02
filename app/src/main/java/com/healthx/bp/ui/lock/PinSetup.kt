package com.healthx.bp.ui.lock

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthx.bp.util.Pin
import kotlinx.coroutines.delay

/**
 * Two-step PIN setup: enter a new 6-digit PIN, then confirm it.
 * Calls [onDone] with the confirmed PIN, or [onCancel] if dismissed.
 */
@Composable
fun PinSetup(onDone: (String) -> Unit, onCancel: () -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    val confirming = first != null

    LaunchedEffect(entered) {
        if (entered.length == Pin.LENGTH) {
            if (!confirming) {
                first = entered
                entered = ""
            } else {
                if (entered == first) {
                    onDone(entered)
                } else {
                    error = true
                    delay(700)
                    first = null
                    entered = ""
                    error = false
                }
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, "取消") }
            }
            Column(
                Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(40.dp))
                Text(
                    if (confirming) "再次输入以确认" else "设置 6 位 PIN 密码",
                    fontSize = 20.sp, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        error -> "两次输入不一致，请重新设置"
                        confirming -> "请再次输入相同的 PIN"
                        else -> "开启后，每次打开 App 需输入此密码"
                    },
                    fontSize = 14.sp,
                    color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(28.dp))
                PinDots(filled = entered.length, error = error)

                Spacer(Modifier.weight(1f))
                Keypad(
                    onDigit = { d -> if (!error && entered.length < Pin.LENGTH) entered += d.toString() },
                    onDelete = { if (!error && entered.isNotEmpty()) entered = entered.dropLast(1) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
