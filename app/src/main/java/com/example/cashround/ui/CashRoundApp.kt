package com.example.cashround.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cashround.ui.about.AboutScreen
import com.example.cashround.ui.history.HistoryScreen
import com.example.cashround.ui.home.HomeScreen
import com.example.cashround.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

enum class AppScreen(
    val route: String,
    val label: String,
    val icon: ImageVector? = null
) {
    Splash("splash", "Splash", null),
    Home("home", "Home", Icons.Default.Home),
    History("history", "History", Icons.Default.History),
    Settings("settings", "Settings", Icons.Default.Settings),
    About("about", "About", Icons.Default.Info)
}

private val bottomNavScreens = listOf(
    AppScreen.Home,
    AppScreen.History,
    AppScreen.Settings,
    AppScreen.About
)

@Composable
fun CashRoundApp(
    viewModel: CashRoundViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppScreen.Splash.route
    var isConfirmationDialogActive by remember { mutableStateOf(false) }

    val showBottomBar = currentRoute != AppScreen.Splash.route && !isConfirmationDialogActive

    fun showMessage(msg: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                screen.icon?.let {
                                    Icon(imageVector = it, contentDescription = screen.label)
                                }
                            },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppScreen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppScreen.Splash.route) {
                SplashScreen(
                    animationsEnabled = uiState.settings.animationsEnabled,
                    onSplashFinished = {
                        navController.navigate(AppScreen.Home.route) {
                            popUpTo(AppScreen.Splash.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(AppScreen.Home.route) {
                HomeScreen(
                    uiState = uiState,
                    onAmountChange = viewModel::onAmountChange,
                    onQuickAmountPreset = viewModel::onQuickAmountPresetSelected,
                    onDenominationPreset = viewModel::onDenominationPresetSelected,
                    onSelectCustomDenomination = viewModel::onSelectCustomDenominationMode,
                    onCustomDenominationChange = viewModel::onCustomDenominationChange,
                    onRoundingModeChange = viewModel::onRoundingModeSelected,
                    onCurrencySelected = viewModel::onCurrencySelected,
                    onCustomCurrencyConfigured = viewModel::onCustomCurrencyConfigured,
                    onDismissCurrencyNotice = viewModel::dismissCurrencyChangeNotice,
                    onRecalculate = viewModel::recalculate,
                    onSaveResult = viewModel::saveCurrentToHistory,
                    onReset = viewModel::resetToDefault,
                    onShowSnackbar = ::showMessage,
                    onCustomDialogVisibilityChanged = { isConfirmationDialogActive = it }
                )
            }

            composable(AppScreen.History.route) {
                HistoryScreen(
                    history = uiState.history,
                    confirmDelete = uiState.settings.confirmHistoryDelete,
                    hapticsEnabled = uiState.settings.hapticFeedback,
                    onItemSelected = { historyItem ->
                        viewModel.restoreFromHistory(historyItem)
                        navController.navigate(AppScreen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        showMessage("Restored rounding calculation")
                    },
                    onDeleteItem = { id ->
                        viewModel.deleteHistoryItem(id)
                        showMessage("Deleted rounding entry")
                    },
                    onClearAll = {
                        viewModel.clearAllHistory()
                        showMessage("Cleared all recent roundings")
                    },
                    onDialogVisibilityChanged = { isConfirmationDialogActive = it }
                )
            }

            composable(AppScreen.Settings.route) {
                SettingsScreen(
                    settings = uiState.settings,
                    onThemeModeChange = viewModel::updateThemeMode,
                    onDynamicColorChange = viewModel::updateDynamicColor,
                    onHapticFeedbackChange = viewModel::updateHapticFeedback,
                    onAnimationsChange = viewModel::updateAnimationsEnabled,
                    onDefaultCurrencyChange = viewModel::updateDefaultCurrency,
                    onDefaultDenominationChange = viewModel::updateDefaultDenomination,
                    onDefaultRoundingModeChange = viewModel::updateDefaultRoundingMode,
                    onConfirmHistoryDeleteChange = viewModel::updateConfirmHistoryDelete,
                    onPreserveLastCalculationChange = viewModel::updatePreserveLastCalculation,
                    onClearHistory = viewModel::clearAllHistory,
                    onResetSettings = viewModel::resetSettingsToDefaults,
                    onShowSnackbar = ::showMessage,
                    onDialogVisibilityChanged = { isConfirmationDialogActive = it }
                )
            }

            composable(AppScreen.About.route) {
                AboutScreen()
            }
        }
    }
}
