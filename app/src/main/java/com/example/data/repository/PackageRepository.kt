package com.example.data.repository

import com.example.data.local.ViralPackage
import com.example.data.local.ViralPackageDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PackageRepository(private val viralPackageDao: ViralPackageDao) {
    val allPackages: Flow<List<ViralPackage>> = viralPackageDao.getAllPackages()

    suspend fun getPackageById(id: Int): ViralPackage? = withContext(Dispatchers.IO) {
        viralPackageDao.getPackageById(id)
    }

    suspend fun insert(viralPackage: ViralPackage): Long = withContext(Dispatchers.IO) {
        viralPackageDao.insertPackage(viralPackage)
    }

    suspend fun deleteById(id: Int) = withContext(Dispatchers.IO) {
        viralPackageDao.deletePackageById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        viralPackageDao.clearAllPackages()
    }
}
