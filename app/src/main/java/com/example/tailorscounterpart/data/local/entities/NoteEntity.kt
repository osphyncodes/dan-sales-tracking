package com.example.tailorscounterpart.data.local.entities

    import androidx.room.Entity
    import androidx.room.ForeignKey
    import androidx.room.Index
    import androidx.room.PrimaryKey
    import java.time.LocalDateTime

@Entity(
        tableName = "notes",
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
    data class  NoteEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0,
        val projectId: Long,
        val dateCreated: LocalDateTime,
        val note: String
    )