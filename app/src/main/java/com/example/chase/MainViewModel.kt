package com.example.chase

import android.location.Location
import androidx.core.location.LocationListenerCompat
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import com.example.chase.api.iss.ISSApi
import kotlinx.coroutines.delay

class MainViewModel(
    private val issApi: ISSApi
) : ViewModel(), LocationListenerCompat {
    private val _gpsLocation = MutableLiveData<Location>()
    internal val gpsLocation = liveData { emitSource(_gpsLocation) }

    internal val issLocation = liveData {
        while (true) {
            val location = getIssLocation()
            location?.let { emit(it) }
            delay(5000L)
        }
    }

    private val _nadirDistance = MediatorLiveData<Float>().apply {
        addSource(gpsLocation) { gpsLocation ->
            value = locationsDistance(gpsLocation, issLocation.value)
        }
        addSource(issLocation) { issLocation ->
            value = locationsDistance(gpsLocation.value, issLocation)
        }
    }
    val nadirDistance = liveData { emitSource(_nadirDistance) }

    override fun onLocationChanged(location: Location) {
        _gpsLocation.value = location
    }

    private suspend fun getIssLocation(): Location? =
        try {
            val position = issApi.issNow().issPosition
            Location("").apply {
                latitude = position.latitude
                longitude = position.longitude
            }
        } catch (ex: Exception) {
            null
        }

    private fun locationsDistance(location1: Location?, location2: Location?): Float? {
        if (location1 == null || location2 == null) return null
        return location1.distanceTo(location2)
    }

    class Factory(private val issApi: ISSApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(issApi) as T
    }
}
