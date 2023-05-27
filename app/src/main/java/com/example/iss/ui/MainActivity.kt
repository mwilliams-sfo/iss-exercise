package com.example.iss.ui

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.iss.R
import com.example.iss.api.iss.ISSApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private val issApi = Retrofit.Builder()
        .baseUrl("http://api.open-notify.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ISSApi::class.java)
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(issApi)
    }

    private lateinit var permissionRequest: ActivityResultLauncher<Array<String>>
    private var locationManager: LocationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lifecycle.addObserver(viewModel)

        permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
        locationManager = getSystemService(LOCATION_SERVICE) as? LocationManager

        viewModel.nadirDistance.observe(this, ::updateNadirDistance)

        val astronautAdapter = StringArrayAdapter()
        val astronautList = findViewById<RecyclerView>(R.id.astronaut_list)
        astronautList.run {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = astronautAdapter
        }
        viewModel.astronauts.observe(this) { names ->
            if (names.isNullOrEmpty()) {
                astronautAdapter.update(listOf(getString(R.string.astronauts_unavailable)))
            } else {
                astronautAdapter.update(names)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun updateNadirDistance(distance: Float?) {
        val nadirValueView = findViewById<TextView>(R.id.nadir_value)
        if (distance == null) {
            nadirValueView.text = getString(R.string.no_data)
        } else {
            val distanceInKm = (distance / 1000).roundToInt()
            nadirValueView.text = getString(R.string.distance_km, distanceInKm)
        }
    }

    private fun startLocationUpdates() {
        val locationManager = locationManager ?: return
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
                viewModel,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        val locationManager = locationManager ?: return
        if (checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED ||
            checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            return
        }
        LocationManagerCompat.removeUpdates(locationManager, viewModel)
    }
}
