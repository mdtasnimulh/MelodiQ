package com.tasnimulhasan.melodiq.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import com.tasnimulhasan.home.navigation.homeScreen
import com.tasnimulhasan.melodiq.ui.MelodiqAppState
import com.tasnimulhasan.settings.navigation.settingsScreen
import com.tasnimulhasan.ui.NavRoutes.HOME_ROUTE

@Composable
fun MelodiqNavHost(
    appState: MelodiqAppState,
    modifier: Modifier = Modifier,
    startDestination: String = HOME_ROUTE,
    navigateToInsert: () -> Unit
) {
    val navController = appState.navController
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        homeScreen()
        settingsScreen()
        /*budgetScreen()
        statisticsScreen()
        insertIncomeExpenseScreen()*/
    }
}