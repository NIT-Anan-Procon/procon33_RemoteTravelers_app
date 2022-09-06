package com.example.procon33_remotetravelers_app.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.example.procon33_remotetravelers_app.BuildConfig
import com.example.procon33_remotetravelers_app.R
import com.example.procon33_remotetravelers_app.databinding.ActivityTravelerBinding
import com.example.procon33_remotetravelers_app.services.SaveCurrentLocationService
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlin.concurrent.thread
import kotlin.properties.Delegates


class TravelerActivity : AppCompatActivity(), OnMapReadyCallback,
    LocationListener, GoogleMap.OnMapClickListener {

    companion object {
        const val CAMERA_REQUEST_CODE = 1
        const val CAMERA_PERMISSION_REQUEST_CODE = 2
    }
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityTravelerBinding
    private lateinit var locationManager: LocationManager
    private lateinit var currentLocation: LatLng
    private var userId by Delegates.notNull<Int>()
    private var track = true
    private var firstLocationChange = true
    private var currentLocationMarker: Marker? = null
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 使用が許可された
            locationStart()
        } else {
            // それでも拒否された時の対応
            val toast = Toast.makeText(this,
                "位置情報の利用を許可してください", Toast.LENGTH_SHORT)
            toast.show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userId = intent.getIntExtra("userId", 0)

        binding = ActivityTravelerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val cameraButton = findViewById<Button>(R.id.camera_button)
        cameraButton.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            resultLauncher.launch(intent)
        }

        val currentLocationButton = findViewById<Button>(R.id.current_location_button)
        currentLocationButton.setOnClickListener {
            if(::mMap.isInitialized && ::currentLocation.isInitialized){
                //ここはバグが起きた時用に一応置いてる
                createMarker()
                track = true
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
            }
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            locationStart()
        }
    }

    @SuppressLint("MissingPermission")
    private fun locationStart() {
        Log.d("debug", "locationStart()")

        // Instances of LocationManager class must be obtained using Context.getSystemService(Class)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (locationManager.isProviderEnabled(GPS_PROVIDER)) {
            Log.d("debug", "location manager Enabled")
        } else {
            // to prompt setting up GPS
            val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(settingsIntent)
            Log.d("debug", "not gpsEnable, startActivity")
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1000)

            Log.d("debug", "checkSelfPermission false")
        } else
            locationManager.requestLocationUpdates(
                GPS_PROVIDER,
                1000,
                20f,
                this)
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = LatLng(location.latitude, location.longitude)
        saveCurrentLocation()
        if(::mMap.isInitialized){
            createMarker()
            if(firstLocationChange){
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
                return
            }
            if(track)
                mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLocation))
        }
    }

    override fun onProviderEnabled(provider: String) {

    }

    override fun onProviderDisabled(provider: String) {

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setMinZoomPreference(9f)
    }

    override fun onMapClick(point: LatLng) {
        track = false
    }

    private fun createMarker(){
        currentLocationMarker?.remove()
        currentLocationMarker = mMap.addMarker(MarkerOptions().position(currentLocation).title("現在地"))
    }

    private fun saveCurrentLocation(){
        val latitude = currentLocation.latitude
        val longitude = currentLocation.longitude
        thread {
            try {
                // APIを実行
                val service: SaveCurrentLocationService =
                    retrofit.create(SaveCurrentLocationService::class.java)
                val saveCurrentLocationResponse = service.saveCurrentLocation(
                    user_id = userId, lat = latitude, lon = longitude, suggestion_flag = 0
                ).execute().body()
                    ?: throw IllegalStateException("body is null")

                Handler(Looper.getMainLooper()).post {
                    // 実行結果を出力
                    Log.d("SaveCurrentLocationResponse", saveCurrentLocationResponse.toString())
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    // エラー内容を出力
                    Log.e("error", e.message.toString())
                }
            }
        }
    }

    private val resultLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            if (data != null) {
                // cancelしたケースも含む
                if (data.extras == null) {
                    return@registerForActivityResult
                }
                else{
                    // CreateReportActivityに写真データを持って遷移する
                    val photo = data.getParcelableExtra<Bitmap>("data")
                    val intent = Intent(this,CreateReportActivity::class.java)
                    intent.putExtra("data", photo)
                    startActivity(intent)
                }
            }
        }
    }
}