package com.example.iss.ui

import android.location.Location
import android.util.Log
import androidx.core.location.LocationListenerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveDataScope
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import com.example.iss.api.iss.ISSApi
import kotlinx.coroutines.delay

class MainViewModel(
    private val issApi: ISSApi
) : ViewModel(), LifecycleEventObserver, LocationListenerCompat {
    private var resumed = false

    private val _gpsLocation = MutableLiveData<Location>()
    internal val gpsLocation = liveData { emitSource(_gpsLocation) }

    internal val issLocation = liveData {pollIssLocation() }

    private val _nadirDistance = MediatorLiveData<Float>().apply {
        addSource(gpsLocation) { gpsLocation ->
            value = locationsDistance(gpsLocation, issLocation.value)
        }
        addSource(issLocation) { issLocation ->
            value = locationsDistance(gpsLocation.value, issLocation)
        }
    }
    val nadirDistance = liveData { emitSource(_nadirDistance) }

    val astronauts = liveData {
        val names = getAstronautNames()
        emit(names)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        resumed = (source.lifecycle.currentState >= Lifecycle.State.RESUMED)
    }

    override fun onLocationChanged(location: Location) {
        _gpsLocation.value = location
    }

    private suspend fun LiveDataScope<Location>.pollIssLocation() {
        while (true) {
            if (resumed) {
                try {
                    val location = getIssLocation()
                    location?.let { emit(it) }
                } catch (ex: Exception) {
                    Log.e(TAG, "Location request failed", ex)
                }
            }
            delay(5000L)
        }
    }

    private suspend fun getIssLocation(): Location? =
        try {
            val position = issApi.issNow().issPosition
            Location("").apply {
                latitude = position.latitude
                longitude = position.longitude
            }
        } catch (ex: Exception) {
            Log.e(TAG, "ISS location request failed", ex)
            null
        }

    private fun locationsDistance(location1: Location?, location2: Location?): Float? {
        if (location1 == null || location2 == null) return null
        return location1.distanceTo(location2)
    }

    private suspend fun getAstronautNames() =
        try {
            issApi.astros().people
                .filter { it.craft == "ISS" }
                .map { it.name }
        } catch (ex: Exception) {
            Log.e(TAG, "Astronaut request failed", ex)
            null
        }

    class Factory(private val issApi: ISSApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(issApi) as T
    }

    companion object {
        const val TAG = "MainViewModel"
    }
}
