package com.healthx.bp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.healthx.bp.ui.export.ExportScreen
import com.healthx.bp.ui.history.HistoryScreen
import com.healthx.bp.ui.home.HomeScreen
import com.healthx.bp.ui.record.RecordScreen
import com.healthx.bp.ui.settings.SettingsScreen
import com.healthx.bp.ui.stats.StatsScreen
import com.healthx.bp.ui.sync.SyncHistoryScreen
import com.healthx.bp.ui.sync.SyncScreen

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBars = currentRoute in topLevelDestinations.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { if (showBars) BottomBar(nav, backStack?.destination) }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(inner)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onAddRecord = { nav.navigate(Routes.record()) },
                    onSeeAll = { nav.navigate(Routes.HISTORY) },
                    onOpenSync = { nav.navigate(Routes.SYNC) }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    onAdd = { nav.navigate(Routes.record()) },
                    onExport = { nav.navigate(Routes.EXPORT) },
                    onEdit = { id -> nav.navigate(Routes.record(id)) }
                )
            }
            composable(Routes.STATS) { StatsScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(onSync = { nav.navigate(Routes.SYNC) })
            }
            composable(
                route = "${Routes.RECORD}?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: -1L
                RecordScreen(recordId = if (id < 0) null else id, onDone = { nav.popBackStack() })
            }
            composable(Routes.SYNC) {
                SyncScreen(
                    onBack = { nav.popBackStack() },
                    onHistory = { nav.navigate(Routes.SYNC_HISTORY) }
                )
            }
            composable(Routes.SYNC_HISTORY) { SyncHistoryScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.EXPORT) { ExportScreen(onBack = { nav.popBackStack() }) }
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController, current: androidx.navigation.NavDestination?) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        NavigationBar(containerColor = Color.Transparent) {
            // Home, History
            topLevelDestinations.take(2).forEach { dest -> NavItem(nav, current, dest) }
            // Center add FAB
            NavigationBarItem(
                selected = false,
                onClick = { nav.navigate(Routes.record()) },
                icon = {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) {
                            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Add, "记录血压", tint = Color.White)
                            }
                        }
                    }
                }
            )
            // Stats, Settings
            topLevelDestinations.drop(2).forEach { dest -> NavItem(nav, current, dest) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(
    nav: NavHostController,
    current: androidx.navigation.NavDestination?,
    dest: TopLevelDest
) {
    val selected = current?.hierarchy?.any { it.route == dest.route } == true
    NavigationBarItem(
        selected = selected,
        onClick = {
            nav.navigate(dest.route) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        icon = { Icon(dest.icon, dest.label, modifier = Modifier.size(22.dp)) },
        label = { Text(dest.label, fontSize = 11.sp) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = Color.Transparent,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
