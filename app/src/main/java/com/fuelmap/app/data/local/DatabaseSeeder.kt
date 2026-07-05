package com.fuelmap.app.data.local

import com.fuelmap.app.data.security.PasswordHasher
import com.fuelmap.app.domain.model.Role

/**
 * Идемпотентный сидинг при первом запуске:
 *  - вшитый супер-администратор (login=sputnik),
 *  - 7 поддерживаемых регионов с границами,
 *  - базовый набор АЗС по регионам.
 */
class DatabaseSeeder(private val db: AppDatabase) {

    suspend fun seed() {
        seedSuperAdmin()
        val regionIds = seedRegions()
        seedStations(regionIds)
    }

    private suspend fun seedSuperAdmin() {
        val userDao = db.userDao()
        if (userDao.getByLogin(SUPER_ADMIN_LOGIN) != null) return
        val salt = PasswordHasher.newSalt()
        userDao.insert(
            UserEntity(
                login = SUPER_ADMIN_LOGIN,
                email = "admin@fuelmap.local",
                passwordHash = PasswordHasher.hash(SUPER_ADMIN_PASSWORD, salt),
                salt = salt,
                carPlate = "А001АА 750",
                region = "Московская область",
                role = Role.SUPER_ADMIN
            )
        )
    }

    /** @return map название региона -> id */
    private suspend fun seedRegions(): Map<String, Long> {
        val regionDao = db.regionDao()
        if (regionDao.count() == 0) {
            regionDao.insertAll(DEFAULT_REGIONS)
        }
        return regionDao.getAll().associate { it.name to it.id }
    }

    private suspend fun seedStations(regionIds: Map<String, Long>) {
        val stationDao = db.stationDao()
        if (stationDao.count() > 0) return
        val stations = DEFAULT_STATIONS.mapNotNull { (regionName, s) ->
            regionIds[regionName]?.let { rid -> s.copy(regionId = rid) }
        }
        stationDao.insertAll(stations)
    }

    companion object {
        const val SUPER_ADMIN_LOGIN = "sputnik"
        const val SUPER_ADMIN_PASSWORD = "Qqwdsaqe2123!"

        private val DEFAULT_REGIONS = listOf(
            RegionEntity(name = "Московская область", minLat = 54.20, minLng = 35.10, maxLat = 56.95, maxLng = 40.25),
            RegionEntity(name = "Краснодарский край", minLat = 43.40, minLng = 36.90, maxLat = 46.75, maxLng = 41.85),
            RegionEntity(name = "Волгоградская область", minLat = 47.50, minLng = 41.10, maxLat = 51.30, maxLng = 47.55),
            RegionEntity(name = "Ростовская область", minLat = 45.90, minLng = 38.20, maxLat = 50.25, maxLng = 44.35),
            RegionEntity(name = "Крым", minLat = 44.30, minLng = 32.45, maxLat = 46.25, maxLng = 36.65),
            RegionEntity(name = "ДНР", minLat = 46.80, minLng = 36.55, maxLat = 49.35, maxLng = 39.05),
            RegionEntity(name = "ЛНР", minLat = 47.80, minLng = 38.00, maxLat = 49.95, maxLng = 40.35)
        )

        // (название региона -> АЗС). regionId проставляется при сидинге.
        private val DEFAULT_STATIONS: List<Pair<String, GasStationEntity>> = listOf(
            // Московская область
            "Московская область" to GasStationEntity(name = "Лукойл АЗС №12", brand = "Лукойл", lat = 55.7522, lng = 37.6156, address = "г. Москва, Садовое кольцо", regionId = 0),
            "Московская область" to GasStationEntity(name = "Газпромнефть", brand = "Газпромнефть", lat = 55.8000, lng = 37.5000, address = "г. Химки, Ленинградское ш.", regionId = 0),
            "Московская область" to GasStationEntity(name = "Роснефть АЗС №48", brand = "Роснефть", lat = 55.6500, lng = 37.7700, address = "г. Люберцы, Октябрьский пр.", regionId = 0),
            "Московская область" to GasStationEntity(name = "Shell", brand = "Shell", lat = 55.9000, lng = 37.3500, address = "г. Зеленоград", regionId = 0),
            // Краснодарский край
            "Краснодарский край" to GasStationEntity(name = "Роснефть", brand = "Роснефть", lat = 45.0355, lng = 38.9753, address = "г. Краснодар, ул. Красная", regionId = 0),
            "Краснодарский край" to GasStationEntity(name = "Лукойл", brand = "Лукойл", lat = 44.7239, lng = 37.7708, address = "г. Новороссийск", regionId = 0),
            "Краснодарский край" to GasStationEntity(name = "Газпромнефть", brand = "Газпромнефть", lat = 43.5855, lng = 39.7231, address = "г. Сочи, Курортный пр.", regionId = 0),
            // Волгоградская область
            "Волгоградская область" to GasStationEntity(name = "Лукойл", brand = "Лукойл", lat = 48.7080, lng = 44.5133, address = "г. Волгоград, пр. Ленина", regionId = 0),
            "Волгоградская область" to GasStationEntity(name = "Роснефть", brand = "Роснефть", lat = 48.7800, lng = 44.7700, address = "г. Волжский", regionId = 0),
            // Ростовская область
            "Ростовская область" to GasStationEntity(name = "Газпромнефть", brand = "Газпромнефть", lat = 47.2357, lng = 39.7015, address = "г. Ростов-на-Дону, Большая Садовая", regionId = 0),
            "Ростовская область" to GasStationEntity(name = "Лукойл", brand = "Лукойл", lat = 47.5167, lng = 42.1667, address = "г. Волгодонск", regionId = 0),
            "Ростовская область" to GasStationEntity(name = "Татнефть", brand = "Татнефть", lat = 47.7080, lng = 40.2300, address = "г. Шахты", regionId = 0),
            // Крым
            "Крым" to GasStationEntity(name = "АТАН АЗС", brand = "АТАН", lat = 44.9521, lng = 34.1024, address = "г. Симферополь, пр. Кирова", regionId = 0),
            "Крым" to GasStationEntity(name = "ТЭС", brand = "ТЭС", lat = 44.6167, lng = 33.5254, address = "г. Севастополь", regionId = 0),
            "Крым" to GasStationEntity(name = "АТАН", brand = "АТАН", lat = 45.3500, lng = 36.4700, address = "г. Керчь", regionId = 0),
            // ДНР
            "ДНР" to GasStationEntity(name = "Параллель АЗС", brand = "Параллель", lat = 48.0159, lng = 37.8028, address = "г. Донецк, пр. Мира", regionId = 0),
            "ДНР" to GasStationEntity(name = "ГАЗПРОМ ДНР", brand = "Газпром", lat = 48.0500, lng = 37.5500, address = "г. Макеевка", regionId = 0),
            // ЛНР
            "ЛНР" to GasStationEntity(name = "Лугансктепловоз АЗС", brand = "ЛТ", lat = 48.5740, lng = 39.3078, address = "г. Луганск, ул. Советская", regionId = 0),
            "ЛНР" to GasStationEntity(name = "АЗС №7", brand = "Народная", lat = 48.6300, lng = 38.7700, address = "г. Алчевск", regionId = 0)
        )
    }
}
