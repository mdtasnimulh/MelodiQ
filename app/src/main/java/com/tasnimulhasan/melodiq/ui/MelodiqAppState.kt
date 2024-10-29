package com.tasnimulhasan.melodiq.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.tasnimulhasan.home.navigation.navigateToHome
import com.tasnimulhasan.melodiq.navigation.TopLevelDestination
import com.tasnimulhasan.settings.navigation.navigateToSettings
import com.tasnimulhasan.ui.NavRoutes.HOME_ROUTE
import com.tasnimulhasan.ui.NavRoutes.SETTINGS_ROUTE
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberMelodiqAppState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberNavController(),
) : MelodiqAppState {
    return remember(
        navController,
        coroutineScope
    ) {
        MelodiqAppState(
            navController = navController,
            coroutineScope = coroutineScope
        )
    }
}

@Stable
class MelodiqAppState(
    val navController: NavHostController,
    coroutineScope: CoroutineScope
) {
    val currentDestination: NavDestination? @Composable get() =
        navController.currentBackStackEntryAsState().value?.destination

    val currentTopLevelDestination: TopLevelDestination?
    @Composable get() =
        when (currentDestination?.route) {
            HOME_ROUTE -> TopLevelDestination.HOME
            SETTINGS_ROUTE -> TopLevelDestination.SETTINGS
            else -> null
        }

    val topLevelDestination: List<TopLevelDestination> = TopLevelDestination.entries

    fun navigateToTopLevelDestination(topLevelDestination: TopLevelDestination) {
        val topLevelNavOptions = navOptions {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }

        when (topLevelDestination) {
            TopLevelDestination.HOME -> navController.navigateToHome(topLevelNavOptions)
            TopLevelDestination.SETTINGS -> navController.navigateToSettings(topLevelNavOptions)
        }
    }

    fun navigateToInsertIncomeExpense() {
        val topLevelNavOptions = navOptions {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }

        //navController.navigateToInsertIncomeExpense(topLevelNavOptions)
    }

    fun navigateBack() {
        navController.navigateUp()
    }
}