package com.fuelmap.app.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object LocationProvider {

    private const val CURRENT_TIMEOUT_MS = 8_000L
    private const val LAST_TIMEOUT_MS = 3_000L

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Возвращает актуальное местоположение. Сначала пытается получить свежий фикс
     * (с таймаутом, чтобы не «зависнуть»), затем — последнее известное. null, если недоступно.
     */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(context: Context): Location? {
        if (!hasPermission(context)) return null
        val client = LocationServices.getFusedLocationProviderClient(context)

        val fresh = withTimeoutOrNull(CURRENT_TIMEOUT_MS) {
            val cts = CancellationTokenSource()
            suspendCancellableCoroutine { cont ->
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
                cont.invokeOnCancellation { cts.cancel() }
            }
        }
        if (fresh != null) return fresh

        // Резерв: последнее известное местоположение.
        return withTimeoutOrNull(LAST_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                client.lastLocation
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
        }
    }
}
