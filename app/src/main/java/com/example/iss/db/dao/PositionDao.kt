package com.example.iss.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.iss.db.entity.Position

@Dao
interface PositionDao {
    @Insert
    fun insert(position: Position)

    @Query("select * from position")
    fun getAll(): LiveData<List<Position>>
}