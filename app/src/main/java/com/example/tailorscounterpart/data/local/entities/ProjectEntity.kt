package com.example.tailorscounterpart.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("customerId"),
        Index("name")
    ]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val dateCreated: LocalDateTime,
    val name: String,
    val dueDate: LocalDateTime,
    val status: String,
    val price: Double,
    val balance: Double = 0.0,
    val isBalanceForgiven: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

fun String.isProjectCompleted(): Boolean =
    equals("Completed", ignoreCase = true) || equals("Complete", ignoreCase = true)

fun String.isProjectInProgress(): Boolean =
    equals("In Progress", ignoreCase = true)

fun String.normalizedProjectStatus(): String = when {
    isProjectCompleted() -> "Completed"
    equals("Cancelled", ignoreCase = true) -> "Cancelled"
    else -> "In Progress"
}