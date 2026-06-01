package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_profiles")
data class GameProfile(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isSystem: Boolean,
    val isAdded: Boolean = false,
    val mode: String = "PERFORMANCE", // PERFORMANCE, BALANCED, BATTERY
    val customFpsTarget: Int = 60,
    val lastLaunched: Long = 0L,
    val accumulatedPlayTimeMs: Long = 0L
)
