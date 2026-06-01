package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameProfileDao {
    @Query("SELECT * FROM game_profiles ORDER BY appName ASC")
    fun getAllProfilesFlow(): Flow<List<GameProfile>>

    @Query("SELECT * FROM game_profiles WHERE isAdded = 1 ORDER BY lastLaunched DESC")
    fun getAddedGamesFlow(): Flow<List<GameProfile>>

    @Query("SELECT * FROM game_profiles WHERE packageName = :packageName LIMIT 1")
    suspend fun getProfileByPackage(packageName: String): GameProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<GameProfile>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: GameProfile)

    @Update
    suspend fun updateProfile(profile: GameProfile)

    @Query("UPDATE game_profiles SET isAdded = :isAdded WHERE packageName = :packageName")
    suspend fun updateAddedStatus(packageName: String, isAdded: Boolean)

    @Query("UPDATE game_profiles SET lastLaunched = :timestamp WHERE packageName = :packageName")
    suspend fun updateLaunchTime(packageName: String, timestamp: Long)

    @Delete
    suspend fun deleteProfile(profile: GameProfile)

    @Query("DELETE FROM game_profiles")
    suspend fun clearAll()
}
