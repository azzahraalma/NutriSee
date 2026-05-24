package com.example.nutrisee.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_history")
data class WeightHistory(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val userId: Int,

    val weight: Float,

    val date: Long
)