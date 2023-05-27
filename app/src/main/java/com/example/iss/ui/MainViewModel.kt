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
import androidx.lifecycle.viewModelScope
import com.example.iss.api.iss.ISSApi
import com.example.iss.api.iss.model.response.ISSNowResponse
import com.example.iss.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.iss.db.entity.Position as DBPosition

class MainViewModel(
    private val issApi: ISSApi,
    private val database: AppDatabase
) : ViewModel(), LifecycleEventObserver, LocationListenerCompat {
    private val locationDao = database.positionDao()
    private var resumed = false

    private val _gpsLocation = MutableLiveData<Location>()
    internal val gpsLocation = liveData { emitSource(_gpsLocation) }

    val issPosition = liveData { pollIssPosition() }

    private val _nadirDistance = MediatorLiveData<Float>().apply {
        addSource(gpsLocation) { gpsLocation ->
            value = locationsDistance(gpsLocation, issPosition.value)
        }
        addSource(issPosition) { issLocation ->
            value = locationsDistance(gpsLocation.value, issLocation)
        }
    }
    val nadirDistance = liveData { emitSource(_nadirDistance) }

    val astronauts = liveData {
        val names = getAstronautNames()
        emit(names)
    }

    val positionLog = locationDao.getAll()

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        resumed = (source.lifecycle.currentState >= Lifecycle.State.RESUMED)
    }

    override fun onLocationChanged(location: Location) {
        _gpsLocation.value = location
    }

    private suspend fun LiveDataScope<Location>.pollIssPosition() {
        while (true) {
            if (resumed) {
                try {
                    val issNow = issApi.issNow()
                    emit(
                        Location("").apply {
                            latitude = issNow.issPosition.latitude
                            longitude = issNow.issPosition.longitude
                        }
                    )
                    logIssPosition(issNow)
                } catch (ex: Exception) {
                    Log.e(TAG, "ISS position request failed", ex)
                }
            }
            delay(5000L)
        }
    }

    private fun logIssPosition(response: ISSNowResponse) {
        viewModelScope.launch(Dispatchers.IO) {
            database.positionDao().insert(
                DBPosition(
                    id = 0,
                    time = response.timestamp,
                    latitude = response.issPosition.latitude,
                    longitude = response.issPosition.longitude
                )
            )
        }
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

    class Factory(
        private val issApi: ISSApi,
        private val database: AppDatabase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(issApi, database) as T
    }

    companion object {
        const val TAG = "MainViewModel"
    }
}
