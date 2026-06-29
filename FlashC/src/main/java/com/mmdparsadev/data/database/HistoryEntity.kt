package com.mmdparsadev.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inputName: String,
    val inputType: String,
    val outputName: String,
    val outputType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean,
    val details: String = ""
)
