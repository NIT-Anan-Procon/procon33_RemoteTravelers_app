package com.example.procon33_remotetravelers_app.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import com.example.procon33_remotetravelers_app.BuildConfig
import com.example.procon33_remotetravelers_app.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.procon33_remotetravelers_app.databinding.ActivityViewerBinding
import com.example.procon33_remotetravelers_app.models.apis.GetInfoResponse
import com.example.procon33_remotetravelers_app.services.GetInfoService
import com.google.android.gms.maps.model.Marker
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*
import kotlin.concurrent.thread
import kotlin.concurrent.scheduleAtFixedRate

class ViewerActivity : AppCompatActivity(), OnMapReadyCallback {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityViewerBinding
    private lateinit var info: GetInfoResponse
    private lateinit var lastLocation: LatLng
    private var track = true
    private var firstTrack = true
    private var currentLocationMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lastLocation = LatLng(0.0, 0.0)
        val userId = intent.getIntExtra("userId", 0)
        Timer().scheduleAtFixedRate(0, 2000){
            getInfo(userId)
            Handler(Looper.getMainLooper()).post {
                if (::info.isInitialized && ::mMap.isInitialized) {
                    displayCurrentLocation()
                }
            }
        }

        binding = ActivityViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val button = findViewById<Button>(R.id.pin_button)
        button.setOnClickListener {
            val intent = Intent(this, SuggestDestinationActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val tokyo = LatLng(35.90684931, 139.68896404)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(tokyo))
    }

    private fun getInfo(userId: Int){
        thread {
            try {
                // APIを実行
                val service: GetInfoService =
                    retrofit.create(GetInfoService::class.java)
                val getInfoResponse = service.getInfo(
                    user_id = userId
                ).execute().body()
                    ?: throw IllegalStateException("body is null")

                Handler(Looper.getMainLooper()).post {
                    // 実行結果を出力
                    Log.d("getInfoResponse", getInfoResponse.toString())
                }
                info = getInfoResponse
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    // エラー内容を出力
                    Log.e("error", e.message.toString())
                }
            }
        }
    }

    private fun displayCurrentLocation(){
        val currentLocation = LatLng(info.current_location.lat, info.current_location.lon)
        if(lastLocation == currentLocation)
            return
        lastLocation = currentLocation
        currentLocationMarker?.remove()
        currentLocationMarker = mMap.addMarker(MarkerOptions().position(currentLocation).title("現在地"))
        if(firstTrack) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
            return
        }
        if(track)
            mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLocation))
    }
}
