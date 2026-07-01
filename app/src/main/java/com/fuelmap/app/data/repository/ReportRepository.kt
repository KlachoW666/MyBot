package com.fuelmap.app.data.repository

import com.fuelmap.app.data.local.MarkDao
import com.fuelmap.app.data.local.ReportDao
import com.fuelmap.app.data.local.ReportEntity
import kotlinx.coroutines.flow.Flow

class ReportRepository(
    private val reportDao: ReportDao,
    private val markDao: MarkDao
) {
    val reports: Flow<List<ReportEntity>> = reportDao.observeAll()
    val reportCount: Flow<Int> = reportDao.observeCount()

    suspend fun report(markId: Long, stationId: Long, userId: Long, reason: String) {
        reportDao.insert(ReportEntity(markId = markId, stationId = stationId, userId = userId, reason = reason))
    }

    /** Отклонить жалобу (метка остаётся). */
    suspend fun dismiss(reportId: Long) = reportDao.delete(reportId)

    /** Удалить метку по жалобе и связанные жалобы. */
    suspend fun deleteMark(markId: Long) {
        markDao.deleteMark(markId)
        reportDao.deleteForMark(markId)
    }
}
