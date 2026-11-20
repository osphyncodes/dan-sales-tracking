package com.example.tailorscounterpart.data.local.database


import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.tailorscounterpart.data.local.daos.ContactDao
import com.example.tailorscounterpart.data.local.daos.ProjectDao
import com.example.tailorscounterpart.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Version 2 only changes Kotlin nullability for the note primary key; the
     * SQLite schema is unchanged. Keeping this explicit migration protects
     * existing on-device records instead of recreating the database.
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) = Unit
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE notes RENAME TO notes_legacy")
            database.execSQL("DROP INDEX IF EXISTS index_notes_projectId")
            database.execSQL(
                """
                CREATE TABLE notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    projectId INTEGER NOT NULL,
                    dateCreated TEXT NOT NULL,
                    note TEXT NOT NULL,
                    FOREIGN KEY(projectId) REFERENCES projects(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            database.execSQL("CREATE INDEX index_notes_projectId ON notes(projectId)")
            database.execSQL(
                """
                INSERT INTO notes (id, projectId, dateCreated, note)
                SELECT old.id, old.projectId, old.dateCreated, old.note
                FROM notes_legacy old
                WHERE EXISTS (SELECT 1 FROM projects p WHERE p.id = old.projectId)
                """.trimIndent()
            )
            database.execSQL("DROP TABLE notes_legacy")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        )
            .addMigrations(MIGRATION_1_2)
            .addMigrations(MIGRATION_2_3)
            .build()
    }

    @Provides
    fun provideDao(db: AppDatabase) = db.customerDao()

    @Provides
    fun provideContactDao(
        database: AppDatabase
    ): ContactDao {
        return database.contactDao()
    }

    @Provides
    fun provideProjectDao(
        database: AppDatabase
    ): ProjectDao {
        return database.projectDao()
    }

    @Provides
    fun provideNoteDao(db: AppDatabase) = db.noteDao()

    @Provides
    fun provideMeasurementDao(db: AppDatabase) = db.measurementDao()

    @Provides
    fun providePaymentDao(db: AppDatabase) = db.paymentDao()

    @Provides
    fun provideProjectMeasurementDao(db: AppDatabase) = db.projectMeasurementDao()
}
