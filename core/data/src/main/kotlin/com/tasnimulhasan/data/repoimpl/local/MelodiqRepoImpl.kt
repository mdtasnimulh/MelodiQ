package com.tasnimulhasan.data.repoimpl.local

import com.tasnimulhasan.database.dao.MelodiQDao
import com.tasnimulhasan.domain.repository.local.MelodiQRepository
import com.tasnimulhasan.entity.room.music.MelodiQEntity
import javax.inject.Inject

class MelodiQRepoImpl @Inject constructor(
    private val melodiQDao: MelodiQDao
) : MelodiQRepository {

    override suspend fun insertIncomeExpense(incomeExpense: MelodiQEntity) {

    }
}