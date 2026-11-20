package com.example.tailorscounterpart.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "customers",
    foreignKeys = [
        ForeignKey(
            entity = VillageEntity::class,
            parentColumns = ["id"],
            childColumns = ["villageId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ClusterEntity::class,
            parentColumns = ["id"],
            childColumns = ["clusterId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateCreated: LocalDateTime,
    val name: String,
    val age: String? = "",
    val clusterId: Long,
    val villageId: Long,
    val phoneNumber: String? = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
