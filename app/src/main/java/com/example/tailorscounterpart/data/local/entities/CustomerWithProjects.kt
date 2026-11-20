package com.example.tailorscounterpart.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation

data class CustomerWithProjects (
    @Embedded
    val customer: CustomerEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "customerId"
    )
    val projects: List<ProjectEntity>
)