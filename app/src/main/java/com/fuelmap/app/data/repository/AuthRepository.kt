package com.fuelmap.app.data.repository

import com.fuelmap.app.data.local.UserDao
import com.fuelmap.app.data.local.UserEntity
import com.fuelmap.app.data.security.PasswordHasher
import com.fuelmap.app.data.session.SessionManager
import com.fuelmap.app.util.PlateValidator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

sealed interface AuthResult {
    data class Success(val user: UserEntity) : AuthResult
    data class Error(val message: String) : AuthResult
}

class AuthRepository(
    private val userDao: UserDao,
    private val session: SessionManager
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: Flow<UserEntity?> =
        session.currentUserId.flatMapLatest { id ->
            if (id == null) flowOf(null) else userDao.observeById(id)
        }

    suspend fun register(
        login: String,
        password: String,
        email: String,
        carPlate: String,
        region: String
    ): AuthResult {
        val l = login.trim()
        when {
            l.length < 3 -> return AuthResult.Error("Логин слишком короткий (мин. 3 символа)")
            password.length < 8 -> return AuthResult.Error("Пароль должен быть не короче 8 символов")
            !email.contains("@") || !email.contains(".") -> return AuthResult.Error("Некорректная почта")
            !PlateValidator.isValid(carPlate) -> return AuthResult.Error("Неверный формат госномера (пример: А123ВС 750)")
            region.isBlank() -> return AuthResult.Error("Выберите регион")
        }
        if (userDao.countByLogin(l) > 0) return AuthResult.Error("Логин уже занят")

        val salt = PasswordHasher.newSalt()
        val user = UserEntity(
            login = l,
            email = email.trim(),
            passwordHash = PasswordHasher.hash(password, salt),
            salt = salt,
            carPlate = PlateValidator.normalize(carPlate),
            region = region
        )
        val id = userDao.insert(user)
        session.signIn(id)
        return AuthResult.Success(user.copy(id = id))
    }

    suspend fun login(loginOrEmail: String, password: String): AuthResult {
        val key = loginOrEmail.trim()
        val user = userDao.getByLoginOrEmail(key, key)
            ?: return AuthResult.Error("Пользователь не найден")
        if (user.isBanned) return AuthResult.Error("Аккаунт заблокирован")
        if (!PasswordHasher.verify(password, user.salt, user.passwordHash)) {
            return AuthResult.Error("Неверный пароль")
        }
        session.signIn(user.id)
        return AuthResult.Success(user)
    }

    /**
     * Сброс пароля по почте. В offline-версии письмо не отправляется: проверяем
     * совпадение логина+почты и сразу задаём новый пароль.
     */
    suspend fun resetPassword(loginOrEmail: String, email: String, newPassword: String): AuthResult {
        if (newPassword.length < 8) return AuthResult.Error("Пароль должен быть не короче 8 символов")
        val key = loginOrEmail.trim()
        val user = userDao.getByLoginOrEmail(key, key)
            ?: return AuthResult.Error("Пользователь не найден")
        if (!user.email.equals(email.trim(), ignoreCase = true)) {
            return AuthResult.Error("Почта не совпадает с указанной при регистрации")
        }
        val salt = PasswordHasher.newSalt()
        val updated = user.copy(salt = salt, passwordHash = PasswordHasher.hash(newPassword, salt))
        userDao.update(updated)
        return AuthResult.Success(updated)
    }

    suspend fun logout() = session.signOut()
}
