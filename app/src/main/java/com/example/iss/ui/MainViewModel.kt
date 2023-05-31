package com.example.iss.ui

import android.location.Location
import android.util.Log
import androidx.core.location.LocationListenerCompat
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.iss.db.entity.Position as DBPosition

private const val issCraftName = "ISS"

class MainViewModel(
    private val issApi: ISSApi,
    private val database: AppDatabase
) : ViewModel(), LocationListenerCompat {
    private val locationDao = database.positionDao()

    private val _gpsLocation = MutableLiveData<Location>()
    internal val gpsLocation = liveData { emitSource(_gpsLocation) }

    private val _issPosition = MutableLiveData<Location>()
    val issPosition = liveData { emitSource(_issPosition) }

    private val _nadirDistance = MediatorLiveData<Float>().apply {
        addSource(gpsLocation) { gpsLocation ->
            value = locationsDistance(gpsLocation, issPosition.value)
        }
        addSource(issPosition) { issLocation ->
            value = locationsDistance(gpsLocation.value, issLocation)
        }
    }
    val nadirDistance = liveData { emitSource(_nadirDistance) }

    private val _astronautNames = MutableLiveData<List<String>>()
    val astronautNames = liveData { emitSource(_astronautNames) }

    val positionLog = locationDao.getAll()

    override fun onLocationChanged(location: Location) {
        _gpsLocation.value = location
    }

    suspend fun updateIssPosition() {
        withContext(Dispatchers.Main) {
            try {
                val response = issApi.issNow()
                _issPosition.value = Location("").apply {
                    latitude = response.issPosition.latitude
                    longitude = response.issPosition.longitude
                }
                logIssPosition(response)
            } catch (ex: Exception) {
                Log.e(TAG, "ISS position request failed", ex)
            }
        }
    }

    private fun locationsDistance(location1: Location?, location2: Location?): Float? {
        if (location1 == null || location2 == null) return null
        return location1.distanceTo(location2)
    }

    suspend fun updateAstronautNames() {
        withContext(Dispatchers.Main) {
            try {
                _astronautNames.value = issApi.astros().people.asSequence()
                    .filter { it.craft == issCraftName }
                    .map { it.name }
                    .toList()
            } catch (ex: Exception) {
                Log.e(TAG, "ISS astronauts request failed", ex)
            }
        }
    }

    private fun logIssPosition(response: ISSNowResponse) {
        viewModelScope.launch(Dispatchers.IO) {
            database.positionDao().insert(
                DBPosition(
                    time = response.timestamp,
                    latitude = response.issPosition.latitude,
                    longitude = response.issPosition.longitude
                )
            )
        }
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
