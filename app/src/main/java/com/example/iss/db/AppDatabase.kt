package com.example.iss.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.iss.db.dao.PositionDao
import com.example.iss.db.entity.Position

@Database(entities = [Position::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun positionDao(): PositionDao
}
