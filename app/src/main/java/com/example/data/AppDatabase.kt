package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromRecordingSource(value: RecordingSource): String = value.name

    @TypeConverter
    fun toRecordingSource(value: String): RecordingSource =
        runCatching { RecordingSource.valueOf(value) }.getOrDefault(RecordingSource.MIC)

    @TypeConverter
    fun fromCallDirection(value: CallDirection): String = value.name

    @TypeConverter
    fun toCallDirection(value: String): CallDirection =
        runCatching { CallDirection.valueOf(value) }.getOrDefault(CallDirection.NONE)

    @TypeConverter
    fun fromCompletenessStatus(value: CompletenessStatus): String = value.name

    @TypeConverter
    fun toCompletenessStatus(value: String): CompletenessStatus =
        runCatching { CompletenessStatus.valueOf(value) }.getOrDefault(CompletenessStatus.MIC_ONLY)

    @TypeConverter
    fun fromBackupSyncStatus(value: BackupSyncStatus): String = value.name

    @TypeConverter
    fun toBackupSyncStatus(value: String): BackupSyncStatus =
        runCatching { BackupSyncStatus.valueOf(value) }.getOrDefault(BackupSyncStatus.LOCAL_ONLY)
}

@Database(entities = [RecordingEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "callmaster_database"
                ).fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
