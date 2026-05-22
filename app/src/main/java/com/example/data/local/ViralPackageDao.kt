package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ViralPackageDao {
    @Query("SELECT * FROM viral_packages ORDER BY timestamp DESC")
    fun getAllPackages(): Flow<List<ViralPackage>>

    @Query("SELECT * FROM viral_packages WHERE id = :id")
    suspend fun getPackageById(id: Int): ViralPackage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackage(viralPackage: ViralPackage): Long

    @Query("DELETE FROM viral_packages WHERE id = :id")
    suspend fun deletePackageById(id: Int)

    @Query("DELETE FROM viral_packages")
    suspend fun clearAllPackages()
}
