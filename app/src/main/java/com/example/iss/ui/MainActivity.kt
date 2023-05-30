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
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.iss.R
import com.example.iss.api.iss.ISSApi
import com.example.iss.db.AppDatabase
import com.example.iss.db.entity.Position
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Clock
import java.time.Instant
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

    private lateinit var permissionRequest: ActivityResultLauncher<Array<String>>
    private var locationManager: LocationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycle.addObserver(viewModel)

        permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
        locationManager = getSystemService(LOCATION_SERVICE) as? LocationManager

        viewModel.nadirDistance.observe(this, ::updateNadirDistance)
        val mapButton = findViewById<AppCompatImageButton>(R.id.show_on_map)
        viewModel.issPosition.observe(this) { position ->
            mapButton.isEnabled = (position != null)
        }
        mapButton.setOnClickListener {
            viewModel.issPosition.value?.let { showLocationOnMap(it, getString(R.string.iss_map_label)) }
        }

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

        val logAdapter = PositionLogAdapter(timeZone = logTimeZone)
        val positionLog = findViewById<RecyclerView>(R.id.position_log)
        positionLog.run {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = logAdapter
        }
        viewModel.positionLog.observe(this) { positions ->
            logAdapter.update(positions?.sortedByDescending { it.time } ?: emptyList())
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

    private fun showLocationOnMap(location: Location, label: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(geoUriOrigin).buildUpon()
                .appendQueryParameter("q", "${location.latitude},${location.longitude}($label)")
                .build()
        )
        if (intent.resolveActivity(packageManager) == null) return
        startActivity(intent)
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
                LocationRequestCompat.Builder(gpsSamplingInterval).build(),
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
