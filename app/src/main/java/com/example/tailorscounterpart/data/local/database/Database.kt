package com.example.tailorscounterpart.data.local.database


import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Database
import com.example.tailorscounterpart.data.local.Converters
import com.example.tailorscounterpart.data.local.daos.ContactDao
import com.example.tailorscounterpart.data.local.daos.CustomerDao
import com.example.tailorscounterpart.data.local.daos.MeasurementDao
import com.example.tailorscounterpart.data.local.daos.NoteDao
import com.example.tailorscounterpart.data.local.daos.PaymentDao
import com.example.tailorscounterpart.data.local.daos.ProjectDao
import com.example.tailorscounterpart.data.local.daos.ProjectMeasurementDao
import com.example.tailorscounterpart.data.local.entities.ContactEntity
import com.example.tailorscounterpart.data.local.entities.CustomerEntity
import com.example.tailorscounterpart.data.local.entities.MeasurementEntity
import com.example.tailorscounterpart.data.local.entities.NoteEntity
import com.example.tailorscounterpart.data.local.entities.PaymentEntity
import com.example.tailorscounterpart.data.local.entities.ProjectEntity
import com.example.tailorscounterpart.data.local.entities.ProjectMeasurementEntity

@Database(
    entities = [
        CustomerEntity::class,
        ProjectEntity::class,
        NoteEntity::class,
        MeasurementEntity::class,
        ProjectMeasurementEntity::class,
        PaymentEntity::class,
        ContactEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase(){
    abstract fun customerDao(): CustomerDao
    abstract fun projectDao(): ProjectDao
    abstract fun noteDao(): NoteDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun projectMeasurementDao(): ProjectMeasurementDao
    abstract fun paymentDao(): PaymentDao
    abstract fun contactDao(): ContactDao
}
