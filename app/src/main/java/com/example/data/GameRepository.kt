package com.example.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GameRepository(
    private val context: Context,
    private val gameProfileDao: GameProfileDao
) {
    val allProfiles: Flow<List<GameProfile>> = gameProfileDao.getAllProfilesFlow()
    val addedGames: Flow<List<GameProfile>> = gameProfileDao.getAddedGamesFlow()

    suspend fun getProfileByPackage(packageName: String): GameProfile? = withContext(Dispatchers.IO) {
        gameProfileDao.getProfileByPackage(packageName)
    }

    suspend fun updateProfile(profile: GameProfile) = withContext(Dispatchers.IO) {
        gameProfileDao.updateProfile(profile)
    }

    suspend fun updateAddedStatus(packageName: String, isAdded: Boolean) = withContext(Dispatchers.IO) {
        gameProfileDao.updateAddedStatus(packageName, isAdded)
    }

    suspend fun updateLaunchTime(packageName: String, timestamp: Long) = withContext(Dispatchers.IO) {
        gameProfileDao.updateLaunchTime(packageName, timestamp)
    }

    suspend fun insertProfile(profile: GameProfile) = withContext(Dispatchers.IO) {
        gameProfileDao.insertProfile(profile)
    }

    /**
     * Scans installed applications, checks if they are games (via API category or package name matching),
     * and seeds the Room database database, avoiding rewriting any custom records.
     */
    suspend fun scanAndStoreInstalledGames() = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            // Query normal applications
            val flags = PackageManager.GET_META_DATA
            val packages = pm.getInstalledApplications(flags)
            
            val profiles = mutableListOf<GameProfile>()
            for (appInfo in packages) {
                // Ensure the app has a launch intent (it's a launchable user app, not a system-only module)
                val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName) ?: continue
                
                // Exclude ourselves
                if (appInfo.packageName == context.packageName) continue

                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val appLabel = appInfo.loadLabel(pm).toString()

                // Check category or name based heuristics
                val isGameCategory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appInfo.category == ApplicationInfo.CATEGORY_GAME
                } else {
                    false
                }

                val lowerPkg = appInfo.packageName.lowercase()
                val lowerLabel = appLabel.lowercase()
                val isProbablyGame = isGameCategory || 
                        lowerPkg.contains("game") || 
                        lowerPkg.contains("pubg") || 
                        lowerPkg.contains("freefire") || 
                        lowerPkg.contains("mlbb") || 
                        lowerPkg.contains("asphalt") || 
                        lowerPkg.contains("genshin") ||
                        lowerLabel.contains("game") ||
                        lowerLabel.contains("steam") ||
                        lowerLabel.contains("simulator") ||
                        lowerLabel.contains("gaming")

                // Check if already exists to preserve customization
                val existing = gameProfileDao.getProfileByPackage(appInfo.packageName)
                if (existing == null) {
                    profiles.add(
                        GameProfile(
                            packageName = appInfo.packageName,
                            appName = appLabel,
                            isSystem = isSystem,
                            isAdded = isProbablyGame, // Auto-add if identified as probable game
                            mode = "PERFORMANCE"
                        )
                    )
                }
            }

            if (profiles.isNotEmpty()) {
                gameProfileDao.insertProfiles(profiles)
                Log.d("GameRepository", "Discovered & seeded ${profiles.size} applications.")
            }
        } catch (e: Exception) {
            Log.e("GameRepository", "Error scanning application packages", e)
        }
    }
}
