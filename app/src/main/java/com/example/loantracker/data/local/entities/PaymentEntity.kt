package com.example.loantracker.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("projectId"),
    ]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val datePaid: LocalDateTime,
    val amountPaid: Double,
    val method: String? = "", //To keep track of payment methods used (Mpamba, Airtel Money, Cash, Bank, Item Exchange)

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)