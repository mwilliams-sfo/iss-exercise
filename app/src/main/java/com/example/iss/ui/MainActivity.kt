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
import android.util.Log
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.iss.R
import com.example.iss.api.iss.ISSApi
import com.example.iss.databinding.ActivityMainBinding
import com.example.iss.databinding.ActivityMainBinding.inflate
import com.example.iss.db.AppDatabase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.roundToInt
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.toDuration

private const val geoUriOrigin = "geo:0,0"
private val locationUpdateInterval = 5000L.toDuration(MILLISECONDS)
private const val issApiBaseUrl = "http://api.open-notify.org/"
private const val appDatabaseName = "app.db"
private val logTimeZone = ZoneId.ofOffset("UTC", ZoneOffset.ofHours(-5))

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

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory(issApi, database) }
    private var issPositionUpdate: Job? = null

    private lateinit var permissionRequest: ActivityResultLauncher<Array<String>>
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflate(layoutInflater)
        setContentView(binding.root)
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
        permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            result.entries.asSequence()
                .filterNot { it.value }
                .forEach { entry -> Log.w(TAG, "Permission denied: ${entry.key}") }
        }
        locationManager = getSystemService(LOCATION_SERVICE) as? LocationManager ?: return
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.upTo(Lifecycle.State.RESUMED) -> startLocationUpdates()
                    Lifecycle.Event.downFrom(Lifecycle.State.RESUMED) -> stopLocationUpdates()
                    else -> Unit
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
            val position = viewModel.issPosition.value ?: return@setOnClickListener
            showLocationOnMap(
                location = position,
                label = getString(R.string.iss_map_label)
            )
        }
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.upTo(Lifecycle.State.RESUMED) -> {
                        issPositionUpdate = lifecycleScope.launch { pollIssPosition() }
                    }
                    Lifecycle.Event.downFrom(Lifecycle.State.RESUMED) -> {
                        issPositionUpdate?.cancel()
                        issPositionUpdate = null
                    }
                    else -> Unit
                }
            }
        )
    }

    private fun initAstronautList() {
        val astronautAdapter = StringArrayAdapter()
        binding.astronautList.run {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = astronautAdapter
        }
        viewModel.astronautNames.observe(this) { names ->
            astronautAdapter.items = names.takeIf { it.isNotEmpty() }
                ?: listOf(getString(R.string.astronauts_unavailable))
        }
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.upTo(Lifecycle.State.RESUMED)) {
                    lifecycleScope.launch { viewModel.updateAstronautNames() }
                }
            }
        )
    }

    private fun initPositionLog() {
        val logAdapter = PositionLogAdapter(timeZone = logTimeZone)
        binding.positionLog.run {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = logAdapter
        }
        viewModel.positionLog.observe(this) { positions ->
            logAdapter.update(items = positions?.sortedByDescending { it.time } ?: emptyList())
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
            Toast.makeText(this, getString(R.string.no_map_application), Toast.LENGTH_LONG)
                .show()
            return
        }
        startActivity(intent)
    }

    private fun startLocationUpdates() {
        if (gpsPermissions.any { permission -> checkSelfPermission(this, permission) != PERMISSION_GRANTED }) {
            permissionRequest.launch(gpsPermissions)
        } else {
            LocationManagerCompat.requestLocationUpdates(
                locationManager,
                LocationManager.GPS_PROVIDER,
                LocationRequestCompat.Builder(locationUpdateInterval.toLong(MILLISECONDS)).build(),
                viewModel,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        if (gpsPermissions.any { permission -> checkSelfPermission(this, permission) != PERMISSION_GRANTED }) {
            return
        }
        LocationManagerCompat.removeUpdates(locationManager, viewModel)
    }

    private suspend fun pollIssPosition() {
        while (true) {
            viewModel.updateIssPosition()
            delay(locationUpdateInterval)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private val gpsPermissions = arrayOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION)
    }
}
