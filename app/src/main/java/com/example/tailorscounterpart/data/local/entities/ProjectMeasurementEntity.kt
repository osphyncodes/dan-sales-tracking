package com.example.tailorscounterpart.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "project_measurements",
    primaryKeys = ["projectId", "measurementId"],
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MeasurementEntity::class,
            parentColumns = ["id"],
            childColumns = ["measurementId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("projectId"),
        Index("measurementId")
    ]
)
data class ProjectMeasurementEntity(
    val projectId: Long,
    val measurementId: Int,

    // Actual value for this project
    val value: Double,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)