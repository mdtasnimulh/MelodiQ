package com.tasnimulhasan.domain.repository.local

import com.tasnimulhasan.entity.room.incomeexpense.MelodiQEntity

interface IncomeExpenseRepository {
    suspend fun insertIncomeExpense(incomeExpense: MelodiQEntity)
}