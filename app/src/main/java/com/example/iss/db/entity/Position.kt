package com.example.iss.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Position(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val time: Long,
    val latitude: Double,
    val longitude: Double
)
