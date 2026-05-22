package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "viral_packages")
data class ViralPackage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val platform: String,
    val niche: String,
    val tone: String,
    val language: String,
    val duration: String,
    val detailLevel: String,
    val timestamp: Long = System.currentTimeMillis(),
    val contentJson: String
)
