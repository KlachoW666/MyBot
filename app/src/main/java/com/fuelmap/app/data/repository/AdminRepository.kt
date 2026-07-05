package com.fuelmap.app.data.repository

import com.fuelmap.app.data.local.UserDao
import com.fuelmap.app.data.local.UserEntity
import com.fuelmap.app.domain.model.Premium
import com.fuelmap.app.domain.model.Role
import kotlinx.coroutines.flow.Flow

class AdminRepository(private val userDao: UserDao) {

    val allUsers: Flow<List<UserEntity>> = userDao.observeAll()

    fun searchUsers(query: String): Flow<List<UserEntity>> = userDao.search(query)

    fun leaderboard(region: String): Flow<List<UserEntity>> = userDao.leaderboard(region)

    suspend fun getUser(id: Long): UserEntity? = userDao.getById(id)

    fun observeUser(id: Long): Flow<UserEntity?> = userDao.observeById(id)

    suspend fun setBanned(user: UserEntity, banned: Boolean): Result<Unit> {
        if (user.role == Role.SUPER_ADMIN) return Result.failure(IllegalStateException("Нельзя забанить супер-администратора"))
        userDao.update(user.copy(isBanned = banned))
        return Result.success(Unit)
    }

    /** Выдать Premium вручную на [days] дней (от текущей даты окончания, если подписка активна). */
    suspend fun grantPremium(user: UserEntity, days: Int = Premium.TRIAL_DAYS) {
        val now = System.currentTimeMillis()
        val base = user.premiumUntil?.takeIf { it > now } ?: now
        userDao.update(user.copy(premiumUntil = base + days * Premium.DAY_MS))
    }

    /** Снять Premium. */
    suspend fun revokePremium(user: UserEntity) {
        userDao.update(user.copy(premiumUntil = null))
    }

    /** Назначение/снятие роли admin. Доступно только супер-администратору (проверяется в VM/UI). */
    suspend fun setAdmin(user: UserEntity, makeAdmin: Boolean): Result<Unit> {
        if (user.role == Role.SUPER_ADMIN) return Result.failure(IllegalStateException("Роль супер-администратора нельзя изменить"))
        val newRole = if (makeAdmin) Role.ADMIN else Role.USER
        userDao.update(user.copy(role = newRole))
        return Result.success(Unit)
    }
}
