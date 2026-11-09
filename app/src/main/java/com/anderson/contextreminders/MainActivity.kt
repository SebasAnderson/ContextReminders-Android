package com.anderson.contextreminders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.anderson.contextreminders.data.UserPreferencesRepository
import com.anderson.contextreminders.ui.AppNavigation
import com.anderson.contextreminders.ui.theme.ContextRemindersTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferencesRepository = UserPreferencesRepository(applicationContext)

        setContent {
            val preferences by preferencesRepository.preferencesFlow.collectAsState(
                initial = null
            )
            val isDark = preferences?.isDarkMode ?: isSystemInDarkTheme()

            ContextRemindersTheme(darkTheme = isDark) {
                AppNavigation()
            }
        }
    }
}
