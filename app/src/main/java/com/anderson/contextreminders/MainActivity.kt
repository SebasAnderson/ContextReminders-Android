package com.anderson.contextreminders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.anderson.contextreminders.ui.AppNavigation
import com.anderson.contextreminders.ui.theme.ContextRemindersTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContextRemindersTheme {
                AppNavigation()
            }
        }
    }
}