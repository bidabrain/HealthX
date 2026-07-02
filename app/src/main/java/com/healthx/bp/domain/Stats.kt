package com.healthx.bp.domain

import com.healthx.bp.data.db.BpRecord

data class BpStats(
    val count: Int,
    val avgSystolic: Int,
    val avgDiastolic: Int,
    val avgHeart: Int,
    val distribution: Map<BpStatus, Int> // count per status
) {
    fun ratio(status: BpStatus): Float =
        if (count == 0) 0f else (distribution[status] ?: 0).toFloat() / count

    companion object {
        val EMPTY = BpStats(0, 0, 0, 0, emptyMap())

        fun from(records: List<BpRecord>): BpStats {
            if (records.isEmpty()) return EMPTY
            val n = records.size
            val avgS = records.sumOf { it.systolic } / n
            val avgD = records.sumOf { it.diastolic } / n
            val avgH = records.sumOf { it.heartRate } / n
            val dist = records.groupingBy { BpStatus.fromKey(it.status) }.eachCount()
            return BpStats(n, avgS, avgD, avgH, dist)
        }
    }
}
