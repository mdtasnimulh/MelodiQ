package com.tasnimulhasan.ui

import com.tasnimulhasan.ui.NavRoutes.BUDGET_ROUTE
import com.tasnimulhasan.ui.NavRoutes.HOME_ROUTE
import com.tasnimulhasan.ui.NavRoutes.INSERT_INCOME_EXPENSE_ROUTE
import com.tasnimulhasan.ui.NavRoutes.SETTINGS_ROUTE
import com.tasnimulhasan.ui.NavRoutes.STATISTICS_ROUTE

object DeepLinks {
    const val DEEP_LINK_HOME = "app://com.melodiq.user/{$HOME_ROUTE}"
    const val DEEP_LINK_BUDGET= "app://com.melodiq.user/{$BUDGET_ROUTE}"
    const val DEEP_LINK_SETTINGS= "app://com.melodiq.user/{$SETTINGS_ROUTE}"
    const val DEEP_LINK_STATISTICS= "app://com.melodiq.user/{$STATISTICS_ROUTE}"
    const val DEEP_LINK_INSERT_INCOME_EXPENSE= "app://com.melodiq.user/{$INSERT_INCOME_EXPENSE_ROUTE}"
}