package com.fuelmap.app.data.repository

import com.fuelmap.app.data.local.GasStationEntity
import com.fuelmap.app.data.local.RegionDao
import com.fuelmap.app.data.local.RegionEntity
import com.fuelmap.app.data.local.StationDao
import com.fuelmap.app.data.local.StationWithCurrentMark
import com.fuelmap.app.data.remote.OsmStationImporter
import kotlinx.coroutines.flow.Flow

class StationRepository(
    private val stationDao: StationDao,
    private val regionDao: RegionDao
) {
    val stations: Flow<List<StationWithCurrentMark>> = stationDao.observeAllWithCurrent()
    val regions: Flow<List<RegionEntity>> = regionDao.observeAll()

    fun observeStation(id: Long): Flow<StationWithCurrentMark?> = stationDao.observeWithCurrent(id)

    suspend fun getStation(id: Long): GasStationEntity? = stationDao.getById(id)

    suspend fun enabledRegions(): List<RegionEntity> = regionDao.getEnabled()

    suspend fun allRegions(): List<RegionEntity> = regionDao.getAll()

    /** Возвращает активный регион, в границах которого находится точка, или null. */
    suspend fun enabledRegionAt(lat: Double, lng: Double): RegionEntity? =
        regionDao.getEnabled().firstOrNull { it.contains(lat, lng) }

    suspend fun setRegionEnabled(region: RegionEntity, enabled: Boolean) =
        regionDao.update(region.copy(enabled = enabled))

    suspend fun addStation(station: GasStationEntity): Long = stationDao.insert(station)

    suspend fun stationCount(): Int = stationDao.count()

    /**
     * Подгружает все АЗС (amenity=fuel) из OpenStreetMap по границам активных регионов.
     * Дубликаты игнорируются по уникальному индексу (lat, lng).
     * @return количество найденных в OSM объектов.
     */
    suspend fun importFromOsm(): Result<Int> = runCatching {
        var total = 0
        for (region in regionDao.getEnabled()) {
            val stations = OsmStationImporter.fetchStations(region)
            if (stations.isNotEmpty()) {
                stationDao.insertAll(stations)
                total += stations.size
            }
        }
        total
    }
}
