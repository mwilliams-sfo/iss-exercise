package com.example.iss.ui

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.iss.R
import com.example.iss.api.iss.ISSApi
import com.example.iss.databinding.ActivityMainBinding
import com.example.iss.db.AppDatabase
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.roundToInt

private const val geoUriOrigin = "geo:0,0"
private const val gpsSamplingInterval = 5000L
private const val issApiBaseUrl = "http://api.open-notify.org/"
private const val appDatabaseName = "app.db"
private val logTimeZone = ZoneId.ofOffset("GMT", ZoneOffset.ofHours(-5))

class MainActivity : AppCompatActivity() {
    private val issApi = Retrofit.Builder()
        .baseUrl(issApiBaseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ISSApi::class.java)

    private val database by lazy {
        Room.databaseBuilder(
            context = applicationContext,
            klass = AppDatabase::class.java,
            name = appDatabaseName
        ).build()
    }

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(issApi, database)
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionRequest: ActivityResultLauncher<Array<String>>
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
            .also { setContentView(it.root) }
        lifecycle.addObserver(viewModel)
        initGpsUpdates()
        initNadir()
        initAstronautList()
        initPositionLog()
    }

    override fun onDestroy() {
        super.onDestroy()
        database.close()
    }

    private fun initGpsUpdates() {
        permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
        locationManager = getSystemService(LOCATION_SERVICE) as? LocationManager ?: return
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event.targetState >= Lifecycle.State.RESUMED) {
                    startLocationUpdates()
                } else {
                    stopLocationUpdates()
                }
            }
        )
    }

    private fun initNadir() {
        viewModel.nadirDistance.observe(this, ::updateNadirDistance)
        viewModel.issPosition.observe(this) { position ->
            binding.showOnMap.isEnabled = (position != null)
        }
        binding.showOnMap.setOnClickListener {
            viewModel.issPosition.value?.let {
                showLocationOnMap(
                    it,
                    getString(R.string.iss_map_label)
                )
            }
        }
    }

    private fun initAstronautList() {
        val astronautAdapter = StringArrayAdapter()
        viewModel.astronauts.observe(this) { names ->
            if (names.isNullOrEmpty()) {
                astronautAdapter.update(listOf(getString(R.string.astronauts_unavailable)))
            } else {
                astronautAdapter.update(names)
            }
        }
        binding.astronautList.run {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = astronautAdapter
        }
    }

    private fun initPositionLog() {
        val logAdapter = PositionLogAdapter(timeZone = logTimeZone)
        viewModel.positionLog.observe(this) { positions ->
            logAdapter.update(positions?.sortedByDescending { it.time } ?: emptyList())
        }
        binding.positionLog.run {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = logAdapter
        }
    }

    private fun updateNadirDistance(distance: Float?) {
        if (distance == null) {
            binding.nadirValue.text = getString(R.string.no_data)
        } else {
            val distanceInKm = (distance / 1000).roundToInt()
            binding.nadirValue.text = getString(R.string.distance_km, distanceInKm)
        }
    }

    private fun showLocationOnMap(location: Location, label: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(geoUriOrigin).buildUpon()
                .appendQueryParameter("q", "${location.latitude},${location.longitude}($label)")
                .build()
        )
        if (intent.resolveActivity(packageManager) == null) {
            Toast.makeText(this, "No map application was found.", Toast.LENGTH_LONG)
                .show()
            return
        }
        startActivity(intent)
    }

    private fun startLocationUpdates() {
        val permissions = arrayOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION)
        if (permissions.any { permission -> checkSelfPermission(this, permission) != PERMISSION_GRANTED }) {
            permissionRequest.launch(permissions)
        } else {
            LocationManagerCompat.requestLocationUpdates(
                locationManager,
                LocationManager.GPS_PROVIDER,
                LocationRequestCompat.Builder(gpsSamplingInterval).build(),
                viewModel,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        val permissions = arrayOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION)
        if (permissions.any { permission -> checkSelfPermission(this, permission) != PERMISSION_GRANTED }) {
            return
        }
        LocationManagerCompat.removeUpdates(locationManager, viewModel)
    }
}
