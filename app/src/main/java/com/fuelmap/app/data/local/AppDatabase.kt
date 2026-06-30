package com.fuelmap.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserEntity::class,
        RegionEntity::class,
        GasStationEntity::class,
        FuelMarkEntity::class,
        FuelMarkItemEntity::class,
        ConfirmationEntity::class,
        FavoriteEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun regionDao(): RegionDao
    abstract fun stationDao(): StationDao
    abstract fun markDao(): MarkDao
    abstract fun confirmationDao(): ConfirmationDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fuelmap.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
