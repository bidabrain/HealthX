package com.healthx.bp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val RECORD = "record"          // record?id={id}
    const val SYNC = "sync"
    const val SYNC_HISTORY = "sync_history"
    const val EXPORT = "export"

    fun record(id: Long? = null) = if (id == null) "record" else "record?id=$id"
}

data class TopLevelDest(val route: String, val label: String, val icon: ImageVector)

val topLevelDestinations = listOf(
    TopLevelDest(Routes.HOME, "首页", Icons.Filled.Home),
    TopLevelDest(Routes.HISTORY, "历史", Icons.Filled.History),
    TopLevelDest(Routes.STATS, "统计", Icons.Filled.BarChart),
    TopLevelDest(Routes.SETTINGS, "设置", Icons.Filled.Settings)
)
