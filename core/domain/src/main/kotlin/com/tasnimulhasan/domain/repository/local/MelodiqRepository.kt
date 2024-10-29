package com.tasnimulhasan.domain.repository.local

import com.tasnimulhasan.entity.room.incomeexpense.IncomeExpenseEntity

interface IncomeExpenseRepository {
    suspend fun insertIncomeExpense(incomeExpense: IncomeExpenseEntity)
}