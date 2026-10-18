package com.example.cashround

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.cashround.repository.CashRoundHistoryRepository
import com.example.cashround.repository.SettingsRepository
import com.example.cashround.ui.CashRoundApp
import com.example.cashround.ui.CashRoundViewModel
import com.example.cashround.ui.theme.CashRoundTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CashRoundViewModel by viewModels {
        val prefs = applicationContext.getSharedPreferences(
            "cash_round_preferences",
            Context.MODE_PRIVATE
        )
        CashRoundViewModel.Factory(
            historyRepository = CashRoundHistoryRepository(prefs),
            settingsRepository = SettingsRepository(prefs)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            CashRoundTheme(
                themeMode = uiState.settings.themeMode,
                dynamicColor = uiState.settings.dynamicColor
            ) {
                CashRoundApp(viewModel = viewModel)
            }
        }
    }
}