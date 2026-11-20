package com.example.loantracker.data.local.data_classes

import androidx.room.ColumnInfo

data class ContactPhonebook(
    val name: String,
    val phoneNumber: String
)

data class ProjectMeasurement(
    val projectId: Long,
    val measurementId: Int,
    val measurementName: String,
    val value: Double,
    val created_at: Long = System.currentTimeMillis()
)