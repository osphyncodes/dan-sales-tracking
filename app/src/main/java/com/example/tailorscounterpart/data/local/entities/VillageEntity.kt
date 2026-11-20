package com.example.tailorscounterpart.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "villages")
data class VillageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)