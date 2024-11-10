package com.tasnimulhasan.domain.repository.local

import com.tasnimulhasan.entity.room.incomeexpense.MelodiQEntity

interface MelodiQRepository {
    suspend fun insertIncomeExpense(incomeExpense: MelodiQEntity)
}