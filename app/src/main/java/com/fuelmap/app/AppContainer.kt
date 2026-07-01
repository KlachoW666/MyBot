package com.fuelmap.app

import android.content.Context
import com.fuelmap.app.data.local.AppDatabase
import com.fuelmap.app.data.repository.AdminRepository
import com.fuelmap.app.data.repository.AuthRepository
import com.fuelmap.app.data.repository.FavoriteRepository
import com.fuelmap.app.data.repository.FuelLogRepository
import com.fuelmap.app.data.repository.MarkRepository
import com.fuelmap.app.data.repository.ReportRepository
import com.fuelmap.app.data.repository.StationRepository
import com.fuelmap.app.data.session.SessionManager
import com.fuelmap.app.data.settings.SettingsManager

/** Простой ручной контейнер зависимостей (без Hilt). */
class AppContainer(context: Context) {
    private val db = AppDatabase.get(context)
    val session = SessionManager(context)
    val settingsManager = SettingsManager(context)

    val authRepository = AuthRepository(db.userDao(), session)
    val stationRepository = StationRepository(db.stationDao(), db.regionDao())
    val markRepository = MarkRepository(db.markDao(), db.confirmationDao(), db.userDao())
    val adminRepository = AdminRepository(db.userDao())
    val favoriteRepository = FavoriteRepository(db.favoriteDao())
    val reportRepository = ReportRepository(db.reportDao(), db.markDao())
    val fuelLogRepository = FuelLogRepository(db.fuelLogDao())

    val database get() = db
}
