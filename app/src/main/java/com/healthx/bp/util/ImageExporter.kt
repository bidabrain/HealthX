package com.healthx.bp.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.healthx.bp.data.db.BpRecord
import com.healthx.bp.domain.BpStats
import com.healthx.bp.domain.BpStatus
import kotlin.math.max
import kotlin.math.min

/**
 * Renders a shareable blood-pressure report image (trend chart + record table)
 * with a pure android.graphics.Canvas so it works off the composition.
 */
object ImageExporter {

    private const val W = 1080
    private const val PAD = 48f

    private val CLR_TEXT = 0xFF1C2330.toInt()
    private val CLR_SUB = 0xFF8A93A6.toInt()
    private val CLR_LINE = 0xFFECEFF3.toInt()
    private val CLR_SYS = 0xFFEF4444.toInt()
    private val CLR_DIA = 0xFF3B82F6.toInt()
    private val CLR_HR = 0xFF22C55E.toInt()

    data class Options(
        val includeChart: Boolean = true,
        val includeTable: Boolean = true,
        val title: String = "血压历史记录",
        val rangeLabel: String
    )

    fun render(recordsAsc: List<BpRecord>, options: Options, generatedAt: Long): Bitmap {
        val stats = BpStats.from(recordsAsc)
        val chartH = if (options.includeChart) 420f else 0f
        val rowH = 60f
        val tableHeaderH = 56f
        val tableH = if (options.includeTable) tableHeaderH + recordsAsc.size * rowH else 0f
        val headerH = 210f
        val footerH = 90f
        val gap = 40f

        var height = PAD + headerH
        if (options.includeChart) height += chartH + gap
        if (options.includeTable) height += tableH + gap
        height += footerH + PAD

        val bmp = Bitmap.createBitmap(W, height.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        var y = PAD + 60f
        // Title
        p.color = CLR_TEXT; p.textSize = 46f; p.typeface = bold; p.textAlign = Paint.Align.CENTER
        canvas.drawText(options.title, W / 2f, y, p)
        y += 44f
        p.color = CLR_SUB; p.textSize = 26f; p.typeface = Typeface.DEFAULT
        canvas.drawText(options.rangeLabel, W / 2f, y, p)
        y += 50f

        // Averages
        p.textAlign = Paint.Align.LEFT
        val avgBoxW = (W - 2 * PAD - 24f) / 2
        drawAvgBox(canvas, p, PAD, y, avgBoxW, "平均血压",
            if (stats.count == 0) "-- / --" else "${stats.avgSystolic} / ${stats.avgDiastolic}", "mmHg", CLR_SYS)
        drawAvgBox(canvas, p, PAD + avgBoxW + 24f, y, avgBoxW, "平均心率",
            if (stats.count == 0) "--" else "${stats.avgHeart}", "次/分", CLR_HR)
        y += 120f + gap

        // Chart
        if (options.includeChart) {
            drawLegend(canvas, p, PAD, y - 8f)
            drawChart(canvas, p, recordsAsc, PAD, y + 8f, W - 2 * PAD, chartH - 16f)
            y += chartH + gap
        }

        // Table
        if (options.includeTable) {
            y = drawTable(canvas, p, bold, recordsAsc, y, rowH, tableHeaderH)
            y += gap
        }

        // Footer
        p.textAlign = Paint.Align.LEFT
        p.color = CLR_SUB; p.textSize = 24f; p.typeface = Typeface.DEFAULT
        canvas.drawText("数据来源：HealthX", PAD, y + 30f, p)
        p.textAlign = Paint.Align.RIGHT
        canvas.drawText("生成时间：${Format.dateTime(generatedAt)}", W - PAD, y + 30f, p)

        return bmp
    }

    private fun drawAvgBox(c: Canvas, p: Paint, x: Float, y: Float, w: Float, label: String, value: String, unit: String, accent: Int) {
        p.color = (accent and 0x00FFFFFF) or 0x14000000
        c.drawRoundRect(RectF(x, y, x + w, y + 120f), 20f, 20f, p)
        p.textAlign = Paint.Align.LEFT
        p.color = CLR_SUB; p.textSize = 24f; p.typeface = Typeface.DEFAULT
        c.drawText(label, x + 24f, y + 40f, p)
        p.color = accent; p.textSize = 44f; p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        c.drawText(value, x + 24f, y + 88f, p)
        val valueW = p.measureText(value)
        p.color = CLR_SUB; p.textSize = 22f; p.typeface = Typeface.DEFAULT
        c.drawText(unit, x + 24f + valueW + 16f, y + 88f, p)
    }

    private fun drawLegend(c: Canvas, p: Paint, x: Float, y: Float) {
        var cx = x
        fun dot(color: Int, label: String) {
            p.color = color
            c.drawCircle(cx + 8f, y, 8f, p)
            p.color = CLR_SUB; p.textSize = 24f; p.textAlign = Paint.Align.LEFT
            c.drawText(label, cx + 24f, y + 8f, p)
            cx += 24f + p.measureText(label) + 40f
        }
        dot(CLR_SYS, "收缩压"); dot(CLR_DIA, "舒张压"); dot(CLR_HR, "心率")
    }

    private fun drawChart(c: Canvas, p: Paint, records: List<BpRecord>, x: Float, y: Float, w: Float, h: Float) {
        if (records.isEmpty()) {
            p.color = CLR_SUB; p.textSize = 28f; p.textAlign = Paint.Align.CENTER
            c.drawText("暂无数据", x + w / 2, y + h / 2, p)
            return
        }
        val leftPad = 70f; val bottomPad = 44f
        val plotX = x + leftPad; val plotW = w - leftPad
        val plotY = y; val plotH = h - bottomPad

        val all = records.flatMap { listOf(it.systolic, it.diastolic, it.heartRate) }
        val minV = (min(all.min(), 40) / 20) * 20
        val maxV = ((max(all.max(), 160) + 19) / 20) * 20
        val range = (maxV - minV).coerceAtLeast(1)

        fun px(i: Int) = if (records.size == 1) plotX + plotW / 2 else plotX + plotW * i / (records.size - 1)
        fun py(v: Int) = plotY + plotH * (1f - (v - minV).toFloat() / range)

        // grid + y labels
        p.textAlign = Paint.Align.LEFT
        val steps = 4
        for (s in 0..steps) {
            val v = minV + range * s / steps
            val gy = py(v)
            p.color = CLR_LINE; p.strokeWidth = 1.5f
            c.drawLine(plotX, gy, x + w, gy, p)
            p.color = CLR_SUB; p.textSize = 22f
            c.drawText(v.toString(), x, gy + 8f, p)
        }
        // x labels
        val labelStep = max(1, records.size / 6)
        p.textAlign = Paint.Align.CENTER; p.color = CLR_SUB; p.textSize = 22f
        var i = 0
        while (i < records.size) {
            c.drawText(Format.monthDay(records[i].timestamp), px(i), y + h - 8f, p)
            i += labelStep
        }

        fun series(sel: (BpRecord) -> Int, color: Int) {
            val path = Path()
            records.forEachIndexed { idx, r ->
                val xx = px(idx); val yy = py(sel(r))
                if (idx == 0) path.moveTo(xx, yy) else path.lineTo(xx, yy)
            }
            p.style = Paint.Style.STROKE; p.color = color; p.strokeWidth = 4f
            p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND
            c.drawPath(path, p)
            p.style = Paint.Style.FILL
            if (records.size <= 40) records.forEachIndexed { idx, r -> c.drawCircle(px(idx), py(sel(r)), 4.5f, p) }
        }
        series({ it.systolic }, CLR_SYS)
        series({ it.diastolic }, CLR_DIA)
        series({ it.heartRate }, CLR_HR)
        p.style = Paint.Style.FILL
    }

    private fun drawTable(c: Canvas, p: Paint, bold: Typeface, records: List<BpRecord>, top: Float, rowH: Float, headerH: Float): Float {
        // columns: date | sys | dia | hr | status
        val cols = floatArrayOf(PAD, PAD + 300f, PAD + 470f, PAD + 640f, PAD + 800f)
        var y = top
        p.textAlign = Paint.Align.LEFT
        // header bg
        p.color = 0xFFF4F6F9.toInt()
        c.drawRect(PAD, y, W - PAD, y + headerH, p)
        p.color = CLR_SUB; p.textSize = 24f; p.typeface = Typeface.DEFAULT
        val headers = listOf("日期时间", "收缩压", "舒张压", "心率", "状态")
        headers.forEachIndexed { idx, t -> c.drawText(t, cols[idx], y + 36f, p) }
        y += headerH

        val desc = records.sortedByDescending { it.timestamp }
        desc.forEach { r ->
            p.color = CLR_LINE; p.strokeWidth = 1f
            c.drawLine(PAD, y, W - PAD, y, p)
            p.typeface = Typeface.DEFAULT; p.textSize = 24f
            p.color = CLR_TEXT
            c.drawText(Format.dateTime(r.timestamp), cols[0], y + 40f, p)
            c.drawText("${r.systolic}", cols[1], y + 40f, p)
            c.drawText("${r.diastolic}", cols[2], y + 40f, p)
            p.color = CLR_SUB
            c.drawText("${r.heartRate}", cols[3], y + 40f, p)
            val st = BpStatus.fromKey(r.status)
            p.color = st.color.toArgbInt()
            c.drawText(st.label, cols[4], y + 40f, p)
            y += rowH
        }
        p.color = CLR_LINE
        c.drawLine(PAD, y, W - PAD, y, p)
        return y
    }

    private fun androidx.compose.ui.graphics.Color.toArgbInt(): Int =
        Color.argb((alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
}
