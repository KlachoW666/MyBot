package com.fuelmap.app.data.repository

import com.fuelmap.app.data.local.FuelLogDao
import com.fuelmap.app.data.local.FuelLogEntity
import kotlinx.coroutines.flow.Flow

class FuelLogRepository(private val dao: FuelLogDao) {

    fun entries(userId: Long): Flow<List<FuelLogEntity>> = dao.observeForUser(userId)

    suspend fun add(userId: Long, liters: Double, cost: Double, odometer: Double) {
        dao.insert(FuelLogEntity(userId = userId, liters = liters, cost = cost, odometer = odometer))
    }

    suspend fun delete(id: Long) = dao.delete(id)
}
