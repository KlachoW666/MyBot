package com.fuelmap.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeById(id: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE login = :login LIMIT 1")
    suspend fun getByLogin(login: String): UserEntity?

    @Query("SELECT * FROM users WHERE login = :login OR email = :email LIMIT 1")
    suspend fun getByLoginOrEmail(login: String, email: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users WHERE login = :login")
    suspend fun countByLogin(login: String): Int

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE login LIKE '%' || :q || '%' OR email LIKE '%' || :q || '%' OR carPlate LIKE '%' || :q || '%' ORDER BY createdAt DESC")
    fun search(q: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE region = :region AND isBanned = 0 ORDER BY karma DESC LIMIT 50")
    fun leaderboard(region: String): Flow<List<UserEntity>>
}

@Dao
interface RegionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(regions: List<RegionEntity>): List<Long>

    @Update
    suspend fun update(region: RegionEntity)

    @Query("SELECT * FROM regions ORDER BY name")
    fun observeAll(): Flow<List<RegionEntity>>

    @Query("SELECT * FROM regions")
    suspend fun getAll(): List<RegionEntity>

    @Query("SELECT * FROM regions WHERE enabled = 1")
    suspend fun getEnabled(): List<RegionEntity>

    @Query("SELECT COUNT(*) FROM regions")
    suspend fun count(): Int
}

@Dao
interface StationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(stations: List<GasStationEntity>): List<Long>

    @Insert
    suspend fun insert(station: GasStationEntity): Long

    @Query("SELECT COUNT(*) FROM gas_stations")
    suspend fun count(): Int

    @Query("SELECT * FROM gas_stations WHERE id = :id")
    suspend fun getById(id: Long): GasStationEntity?

    @Transaction
    @Query("SELECT * FROM gas_stations WHERE status = 'APPROVED'")
    fun observeAllWithCurrent(): Flow<List<StationWithCurrentMark>>

    @Transaction
    @Query("SELECT * FROM gas_stations WHERE id = :id")
    fun observeWithCurrent(id: Long): Flow<StationWithCurrentMark?>

    @Query("SELECT * FROM gas_stations WHERE status = 'PENDING' ORDER BY id DESC")
    fun observePending(): Flow<List<GasStationEntity>>

    @Query("SELECT COUNT(*) FROM gas_stations WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT DISTINCT brand FROM gas_stations WHERE status = 'APPROVED' AND brand != '' ORDER BY brand")
    fun observeBrands(): Flow<List<String>>

    @Query("UPDATE gas_stations SET status = 'APPROVED' WHERE id = :id")
    suspend fun approve(id: Long)

    @Query("DELETE FROM gas_stations WHERE id = :id")
    suspend fun deleteStation(id: Long)
}

@Dao
interface MarkDao {
    @Insert
    suspend fun insertMark(mark: FuelMarkEntity): Long

    @Insert
    suspend fun insertItems(items: List<FuelMarkItemEntity>)

    @Update
    suspend fun updateMark(mark: FuelMarkEntity)

    @Query("UPDATE fuel_marks SET isCurrent = 0 WHERE stationId = :stationId AND isCurrent = 1")
    suspend fun clearCurrentForStation(stationId: Long)

    @Query("SELECT * FROM fuel_marks WHERE id = :id")
    suspend fun getMark(id: Long): FuelMarkEntity?

    @Query("DELETE FROM fuel_marks WHERE id = :id")
    suspend fun deleteMark(id: Long)

    @Transaction
    @Query("SELECT * FROM fuel_marks WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeUserHistory(userId: Long): Flow<List<MarkWithItems>>

    @Transaction
    @Query("SELECT * FROM fuel_marks WHERE stationId = :stationId ORDER BY createdAt ASC")
    fun observeStationHistory(stationId: Long): Flow<List<MarkWithItems>>

    /**
     * Атомарно сохраняет новую отметку как актуальную: сбрасывает прежний актуальный
     * статус АЗС и вставляет новый с его типами топлива.
     */
    @Transaction
    suspend fun saveCurrentMark(mark: FuelMarkEntity, items: List<FuelMarkItemEntity>): Long {
        clearCurrentForStation(mark.stationId)
        val markId = insertMark(mark.copy(isCurrent = true))
        insertItems(items.map { it.copy(markId = markId) })
        return markId
    }
}

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND stationId = :stationId")
    suspend fun remove(userId: Long, stationId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND stationId = :stationId)")
    suspend fun isFavorite(userId: Long, stationId: Long): Boolean

    @Query("SELECT stationId FROM favorites WHERE userId = :userId")
    fun observeStationIds(userId: Long): Flow<List<Long>>
}

@Dao
interface ConfirmationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(confirmation: ConfirmationEntity): Long

    @Query("SELECT COUNT(*) FROM confirmations WHERE markId = :markId AND userId = :userId")
    suspend fun countForUser(markId: Long, userId: Long): Int
}
