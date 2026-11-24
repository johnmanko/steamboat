package com.johnmanko.app.steamboat.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * The newest fix the device already has, or one fresh reading if it has none.
 * Call only after location permission is granted.
 */
@SuppressLint("MissingPermission")
suspend fun currentCoordinates(context: Context): Pair<Double, Double>? {
    val manager = context.getSystemService(LocationManager::class.java) ?: return null
    val providers = providers(manager)
    val last = providers.mapNotNull { provider ->
        runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
    }.maxByOrNull { it.time }
    val location = last ?: providers.firstNotNullOfOrNull { provider ->
        withTimeoutOrNull(8_000) { awaitLocation(manager, provider) }
    } ?: return null
    return location.latitude to location.longitude
}

private fun providers(manager: LocationManager): List<String> = buildList {
    if (Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
    add(LocationManager.GPS_PROVIDER)
    add(LocationManager.NETWORK_PROVIDER)
}.filter { provider ->
    runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
}

@SuppressLint("MissingPermission")
private suspend fun awaitLocation(manager: LocationManager, provider: String): Location? =
    suspendCancellableCoroutine { cont ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                manager.removeUpdates(this)
                if (cont.isActive) cont.resume(location)
            }

            override fun onProviderDisabled(provider: String) {
                manager.removeUpdates(this)
                if (cont.isActive) cont.resume(null)
            }
        }
        cont.invokeOnCancellation { manager.removeUpdates(listener) }
        try {
            manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        } catch (error: RuntimeException) {
            if (cont.isActive) cont.resume(null)
        }
    }
