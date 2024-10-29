package com.tasnimulhasan.data.repoimpl.local

import com.tasnimulhasan.database.dao.IncomeExpenseDao
import com.tasnimulhasan.domain.repository.local.IncomeExpenseRepository
import com.tasnimulhasan.entity.room.incomeexpense.IncomeExpenseEntity
import javax.inject.Inject

class IncomeExpenseRepoImpl @Inject constructor(
    private val incomeExpenseDao: IncomeExpenseDao
) : IncomeExpenseRepository {

    override suspend fun insertIncomeExpense(incomeExpense: IncomeExpenseEntity) {

    }
}