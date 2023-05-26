package com.example.chase

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import androidx.lifecycle.lifecycleScope
import com.example.chase.api.iss.ISSApi
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var issApi: ISSApi

    private lateinit var permissionRequest: ActivityResultLauncher<Array<String>>
    private lateinit var locationManager: LocationManager
    private val locationListener: LocationListenerCompat = LocationListenerCompat { location ->
        onLocationChanged(location)
    }

    private var currentLocation: Location? = null
    private var issLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        issApi = Retrofit.Builder()
            .baseUrl("http://api.open-notify.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(ISSApi::class.java)
    }

    override fun onResume() {
        super.onResume()

        if (checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED ||
                checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            permissionRequest.launch(
                arrayOf(
                    ACCESS_COARSE_LOCATION,
                    ACCESS_FINE_LOCATION
                )
            )
        } else {
            LocationManagerCompat.requestLocationUpdates(
                locationManager,
                LocationManager.GPS_PROVIDER,
                LocationRequestCompat.Builder(5000L).build(),
                locationListener,
                Looper.getMainLooper()
            )
        }
        lifecycleScope.launch {
            val position = issApi.issNow().issPosition
            issLocation = Location("").apply {
                latitude = position.latitude
                longitude = position.longitude
            }
            updateDistance()
        }
    }

    override fun onPause() {
        super.onPause()
        locationManager.removeUpdates(locationListener)
    }

    private fun onLocationChanged(location: Location) {
        currentLocation = location
        updateDistance()
    }

    private fun updateDistance() {
        val currentLocationVal = currentLocation ?: return
        val issLocationVal = issLocation ?: return
        val distance = currentLocationVal.distanceTo(issLocationVal)
        val distanceInKm = (distance / 1000).roundToInt()
        findViewById<TextView>(R.id.nadir_value).text = "$distanceInKm km"
    }
}
