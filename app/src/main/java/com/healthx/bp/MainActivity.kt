package com.healthx.bp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthx.bp.data.prefs.AppSettings
import com.healthx.bp.ui.lock.LockScreen
import com.healthx.bp.ui.navigation.AppRoot
import com.healthx.bp.ui.theme.HealthXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = (application as HealthXApp).graph
        setContent {
            val settings by graph.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            val locked by graph.appLock.locked.collectAsStateWithLifecycle()
            HealthXTheme(themeMode = settings.themeMode) {
                if (settings.lockActive && locked) {
                    LockScreen(
                        salt = settings.pinSalt,
                        expectedHash = settings.pinHash,
                        onUnlocked = { graph.appLock.unlock() }
                    )
                } else {
                    AppRoot()
                }
            }
        }
    }
}
