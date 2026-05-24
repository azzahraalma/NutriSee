package com.example.nutrisee.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.nutrisee.data.dao.MakananDao
import com.example.nutrisee.data.entity.MakananEntry

@Database(
    entities = [
        User::class,
        UserProfile::class,
        WeightHistory::class,
        FoodLog::class,
        MakananEntry::class,
        ActivityLog::class,
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun weightHistoryDao(): WeightHistoryDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun makananDao(): MakananDao
    abstract fun activityLogDao(): ActivityLogDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nutrisee_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}