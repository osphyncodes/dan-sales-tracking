package com.example.loantracker.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clusters")
data class ClusterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)