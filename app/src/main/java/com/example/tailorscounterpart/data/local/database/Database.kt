package com.example.tailorscounterpart.data.local.database


import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Database
import com.example.tailorscounterpart.data.local.Converters
import com.example.tailorscounterpart.data.local.daos.ClusterDao
import com.example.tailorscounterpart.data.local.daos.CustomerDao
import com.example.tailorscounterpart.data.local.daos.VillageDao
import com.example.tailorscounterpart.data.local.daos.NoteDao
import com.example.tailorscounterpart.data.local.daos.PaymentDao
import com.example.tailorscounterpart.data.local.daos.ProjectDao
import com.example.tailorscounterpart.data.local.entities.ClusterEntity
import com.example.tailorscounterpart.data.local.entities.CustomerEntity
import com.example.tailorscounterpart.data.local.entities.NoteEntity
import com.example.tailorscounterpart.data.local.entities.PaymentEntity
import com.example.tailorscounterpart.data.local.entities.ProjectEntity
import com.example.tailorscounterpart.data.local.entities.VillageEntity

@Database(
    entities = [
        CustomerEntity::class,
        ProjectEntity::class,
        NoteEntity::class,
        VillageEntity::class,
        ClusterEntity::class,
        PaymentEntity::class,
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase(){
    abstract fun customerDao(): CustomerDao
    abstract fun projectDao(): ProjectDao
    abstract fun noteDao(): NoteDao

    abstract fun clusterDao(): ClusterDao
    abstract fun villageDao(): VillageDao
    abstract fun paymentDao(): PaymentDao
}
