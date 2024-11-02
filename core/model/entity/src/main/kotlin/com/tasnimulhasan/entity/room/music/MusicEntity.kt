package com.tasnimulhasan.entity.room.incomeexpense

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "income_expense_table")
data class MelodiQEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
)