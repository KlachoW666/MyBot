package com.fuelmap.app.data.repository

import com.fuelmap.app.data.local.FavoriteDao
import com.fuelmap.app.data.local.FavoriteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoriteRepository(private val dao: FavoriteDao) {

    fun favorites(userId: Long): Flow<Set<Long>> =
        dao.observeStationIds(userId).map { it.toSet() }

    suspend fun toggle(userId: Long, stationId: Long) {
        if (dao.isFavorite(userId, stationId)) {
            dao.remove(userId, stationId)
        } else {
            dao.add(FavoriteEntity(userId = userId, stationId = stationId))
        }
    }
}
