package com.healthx.bp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthx.bp.data.db.BpRecord
import com.healthx.bp.ui.theme.BpDiastolic
import com.healthx.bp.ui.theme.BpHeart
import com.healthx.bp.ui.theme.BpSystolic
import com.healthx.bp.ui.theme.Divider
import com.healthx.bp.ui.theme.TextSecondary
import com.healthx.bp.util.Format
import kotlin.math.max
import kotlin.math.min

data class ChartSeries(val values: List<Int>, val color: Color, val label: String)

/**
 * Multi-line trend chart. [records] must be sorted ascending by time.
 * [showHeart] toggles the heart-rate line (hidden on the stats "趋势" mini chart).
 */
@Composable
fun BpLineChart(
    records: List<BpRecord>,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    showHeart: Boolean = true
) {
    val axisColor = Divider
    val labelColor = TextSecondary
    val density = LocalDensity.current

    if (records.isEmpty()) {
        Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
            Text("暂无数据", color = TextSecondary, fontWeight = FontWeight.Normal)
        }
        return
    }

    val systolic = records.map { it.systolic }
    val diastolic = records.map { it.diastolic }
    val heart = records.map { it.heartRate }

    val allValues = buildList {
        addAll(systolic); addAll(diastolic); if (showHeart) addAll(heart)
    }
    val rawMin = allValues.min()
    val rawMax = allValues.max()
    // pad and snap to a nice grid
    val minV = (min(rawMin, 40) / 20) * 20
    val maxV = ((max(rawMax, 160) + 19) / 20) * 20
    val range = (maxV - minV).coerceAtLeast(1)

    Canvas(modifier.fillMaxWidth().height(height).padding(top = 8.dp, bottom = 4.dp)) {
        val leftPad = 34.dp.toPx()
        val bottomPad = 22.dp.toPx()
        val rightPad = 8.dp.toPx()
        val topPad = 6.dp.toPx()
        val chartW = size.width - leftPad - rightPad
        val chartH = size.height - topPad - bottomPad

        fun xAt(i: Int): Float =
            if (records.size == 1) leftPad + chartW / 2
            else leftPad + chartW * i / (records.size - 1)

        fun yAt(v: Int): Float = topPad + chartH * (1f - (v - minV).toFloat() / range)

        // horizontal grid + y labels
        val steps = 4
        val txtPaint = android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            isAntiAlias = true
        }
        for (s in 0..steps) {
            val v = minV + range * s / steps
            val y = yAt(v)
            drawLine(axisColor, Offset(leftPad, y), Offset(size.width - rightPad, y), 1f)
            drawContext.canvas.nativeCanvas.drawText(
                v.toString(), 2f, y + txtPaint.textSize / 3, txtPaint
            )
        }

        // x labels (up to ~6)
        val labelCount = min(6, records.size)
        if (labelCount > 0) {
            val stepIdx = max(1, (records.size - 1) / max(1, labelCount - 1))
            var i = 0
            while (i < records.size) {
                val x = xAt(i)
                val label = Format.monthDay(records[i].timestamp)
                val w = txtPaint.measureText(label)
                drawContext.canvas.nativeCanvas.drawText(
                    label, x - w / 2, size.height - 4f, txtPaint
                )
                i += stepIdx
            }
        }

        // series
        fun drawSeries(values: List<Int>, color: Color) {
            val path = Path()
            values.forEachIndexed { i, v ->
                val p = Offset(xAt(i), yAt(v))
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
            if (records.size <= 40) {
                values.forEachIndexed { i, v ->
                    drawCircle(color, 2.2.dp.toPx(), Offset(xAt(i), yAt(v)))
                }
            }
        }

        drawSeries(systolic, BpSystolic)
        drawSeries(diastolic, BpDiastolic)
        if (showHeart) drawSeries(heart, BpHeart)
    }
}

@Composable
fun ChartLegend(showHeart: Boolean = true, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendDot(BpSystolic, "收缩压")
        LegendDot(BpDiastolic, "舒张压")
        if (showHeart) LegendDot(BpHeart, "心率")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

/** Donut chart for status distribution. [slices] = list of (color, value). */
@Composable
fun DonutChart(
    slices: List<Pair<Color, Float>>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 18.dp
) {
    val total = slices.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(0.0001f)
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val inset = stroke / 2
        val arcSize = androidx.compose.ui.geometry.Size(size.minDimension - stroke, size.minDimension - stroke)
        val topLeft = Offset(
            (size.width - arcSize.width) / 2,
            (size.height - arcSize.height) / 2
        )
        var start = -90f
        // track
        drawArc(Divider, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
        slices.forEach { (color, value) ->
            if (value <= 0f) return@forEach
            val sweep = 360f * value / total
            drawArc(color, start, sweep - 1.5f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            start += sweep
        }
    }
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
)
