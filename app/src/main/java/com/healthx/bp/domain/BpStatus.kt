package com.healthx.bp.domain

import androidx.compose.ui.graphics.Color
import com.healthx.bp.ui.theme.BpHigh
import com.healthx.bp.ui.theme.BpHighNormal
import com.healthx.bp.ui.theme.BpLow
import com.healthx.bp.ui.theme.BpNormal
import com.healthx.bp.ui.theme.BpWarn

/**
 * Blood-pressure status categories (aligned with the Chinese hypertension
 * guideline, 5 levels). Stored in the DB as the [key] string so the enum can be
 * reordered safely. Diagnostic thresholds are fixed for adults — they do not
 * vary by age or sex (see project notes).
 *
 * Enum order matters: [classify] takes the higher of the systolic/diastolic
 * categories, which relies on the ordinal ranking below.
 */
enum class BpStatus(val key: String, val label: String, val color: Color) {
    LOW("low", "偏低", BpLow),                     // < 90/60
    NORMAL("normal", "正常", BpNormal),            // < 120/80
    HIGH_NORMAL("high_normal", "正常高值", BpHighNormal), // 120–139 / 80–89
    ELEVATED("elevated", "偏高", BpWarn),          // 140–159 / 90–99  (高血压1级)
    HIGH("high", "高", BpHigh);                    // ≥ 160/100        (高血压2/3级)

    companion object {
        fun fromKey(key: String?): BpStatus =
            entries.firstOrNull { it.key == key } ?: NORMAL

        private fun systolicCategory(s: Int): BpStatus = when {
            s < 90 -> LOW
            s < 120 -> NORMAL
            s < 140 -> HIGH_NORMAL
            s < 160 -> ELEVATED
            else -> HIGH
        }

        private fun diastolicCategory(d: Int): BpStatus = when {
            d < 60 -> LOW
            d < 80 -> NORMAL
            d < 90 -> HIGH_NORMAL
            d < 100 -> ELEVATED
            else -> HIGH
        }

        /**
         * Classify a reading. When systolic and diastolic fall into different
         * categories, the higher one wins (guideline rule). "偏低" applies only
         * when both values are low.
         */
        fun classify(systolic: Int, diastolic: Int): BpStatus {
            val s = systolicCategory(systolic)
            val d = diastolicCategory(diastolic)
            if (s == LOW && d == LOW) return LOW
            // A lone low value is treated as NORMAL for the purpose of taking the max.
            val sRank = if (s == LOW) NORMAL.ordinal else s.ordinal
            val dRank = if (d == LOW) NORMAL.ordinal else d.ordinal
            return entries[maxOf(sRank, dRank)]
        }
    }
}
