package com.fuelmap.app.data.repository

import com.fuelmap.app.data.local.ConfirmationDao
import com.fuelmap.app.data.local.ConfirmationEntity
import com.fuelmap.app.data.local.FuelMarkEntity
import com.fuelmap.app.data.local.FuelMarkItemEntity
import com.fuelmap.app.data.local.MarkDao
import com.fuelmap.app.data.local.MarkWithItems
import com.fuelmap.app.data.local.UserDao
import com.fuelmap.app.domain.model.ConfirmationType
import com.fuelmap.app.domain.model.FuelType
import com.fuelmap.app.domain.model.Queue
import kotlinx.coroutines.flow.Flow

/** Один выбранный тип топлива в форме отметки. */
data class FuelEntry(
    val type: FuelType,
    val available: Boolean,
    val price: Double
)

class MarkRepository(
    private val markDao: MarkDao,
    private val confirmationDao: ConfirmationDao,
    private val userDao: UserDao
) {
    fun userHistory(userId: Long): Flow<List<MarkWithItems>> = markDao.observeUserHistory(userId)

    fun stationHistory(stationId: Long): Flow<List<MarkWithItems>> = markDao.observeStationHistory(stationId)

    /** Сохраняет новую актуальную отметку (серверное время = System.currentTimeMillis()). */
    suspend fun submitMark(
        stationId: Long,
        userId: Long,
        entries: List<FuelEntry>,
        queue: Queue,
        photoUri: String? = null
    ): Result<Long> {
        if (entries.isEmpty()) return Result.failure(IllegalArgumentException("Выберите хотя бы один тип топлива"))
        val mark = FuelMarkEntity(stationId = stationId, userId = userId, queue = queue, photoUri = photoUri)
        val items = entries.map {
            FuelMarkItemEntity(markId = 0, type = it.type, available = it.available, price = it.price)
        }
        val id = markDao.saveCurrentMark(mark, items)
        return Result.success(id)
    }

    /**
     * Подтверждение достоверности отметки. Один пользователь — один голос на отметку.
     * За подтверждение «топливо есть» автору начисляется карма.
     */
    suspend fun confirm(markId: Long, userId: Long, type: ConfirmationType): Result<Unit> {
        if (confirmationDao.countForUser(markId, userId) > 0) {
            return Result.failure(IllegalStateException("Вы уже оценивали эту отметку"))
        }
        val mark = markDao.getMark(markId) ?: return Result.failure(IllegalStateException("Отметка не найдена"))
        confirmationDao.insert(ConfirmationEntity(markId = markId, userId = userId, type = type))
        when (type) {
            ConfirmationType.CONFIRM -> {
                markDao.updateMark(mark.copy(confirmCount = mark.confirmCount + 1))
                if (mark.userId != userId) addKarma(mark.userId, 1)
            }
            ConfirmationType.EMPTY -> {
                markDao.updateMark(mark.copy(emptyCount = mark.emptyCount + 1))
            }
        }
        return Result.success(Unit)
    }

    private suspend fun addKarma(userId: Long, delta: Int) {
        val u = userDao.getById(userId) ?: return
        userDao.update(u.copy(karma = u.karma + delta))
    }

    suspend fun deleteMark(markId: Long) = markDao.deleteMark(markId)
}
