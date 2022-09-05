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
import com.example.procon33_remotetravelers_app.R
import com.example.procon33_remotetravelers_app.databinding.ActivityTravelerBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions


class TravelerActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {

    companion object {
        const val CAMERA_REQUEST_CODE = 1
        const val CAMERA_PERMISSION_REQUEST_CODE = 2
    }

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityTravelerBinding
    private lateinit var locationManager: LocationManager
    private lateinit var currentLocation: LatLng
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

        val testButton = findViewById<Button>(R.id.current_location_button)
        testButton.setOnClickListener {
            if(::mMap.isInitialized && ::currentLocation.isInitialized){
                currentLocationMarker?.remove()
                currentLocationMarker = mMap.addMarker(MarkerOptions().position(currentLocation).title("現在地"))
                mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLocation))
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
                25f,
                this)
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = LatLng(location.latitude, location.longitude)
        if(::mMap.isInitialized){
            currentLocationMarker?.remove()
            currentLocationMarker = mMap.addMarker(MarkerOptions().position(currentLocation).title("現在地"))
        }
    }

    override fun onProviderEnabled(provider: String) {

    }

    override fun onProviderDisabled(provider: String) {

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setMinZoomPreference(8f)
        currentLocationMarker?.remove()
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
                    intent.putExtra("data",photo)
                    startActivity(intent)
                }
            }
        }
    }
}