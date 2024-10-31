package com.tasnimulhasan.data.repoimpl.local

import com.tasnimulhasan.database.dao.MelodiQDao
import com.tasnimulhasan.domain.repository.local.IncomeExpenseRepository
import com.tasnimulhasan.entity.room.incomeexpense.MelodiQEntity
import javax.inject.Inject

class IncomeExpenseRepoImpl @Inject constructor(
    private val melodiQDao: MelodiQDao
) : IncomeExpenseRepository {

    override suspend fun insertIncomeExpense(incomeExpense: MelodiQEntity) {

    }
}