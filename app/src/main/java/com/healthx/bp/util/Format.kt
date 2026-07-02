package com.healthx.bp.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Format {
    private val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val dateOnly = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthDay = SimpleDateFormat("MM-dd", Locale.getDefault())
    private val timeOnly = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun dateTime(ts: Long): String = dateTime.format(Date(ts))
    fun date(ts: Long): String = dateOnly.format(Date(ts))
    fun monthDay(ts: Long): String = monthDay.format(Date(ts))
    fun time(ts: Long): String = timeOnly.format(Date(ts))
}

/** Time ranges used by history / stats / export filters. */
enum class TimeRange(val label: String, val days: Int) {
    D7("7天", 7),
    D30("30天", 30),
    D90("90天", 90),
    ALL("全部", -1);

    /** Epoch-millis lower bound for this range (0 == include everything). */
    fun startMillis(now: Long = System.currentTimeMillis()): Long {
        if (days < 0) return 0L
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return cal.timeInMillis
    }
}
