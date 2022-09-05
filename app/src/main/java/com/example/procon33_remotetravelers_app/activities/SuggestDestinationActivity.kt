package com.example.procon33_remotetravelers_app.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.example.procon33_remotetravelers_app.BuildConfig
import com.example.procon33_remotetravelers_app.R

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.procon33_remotetravelers_app.databinding.ActivitySuggestDestinationBinding
import com.example.procon33_remotetravelers_app.services.SuggestDestinationService
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlin.concurrent.thread

class SuggestDestinationActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnMapClickListener {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivitySuggestDestinationBinding
    private lateinit var suggestDestination: LatLng
    private var suggestMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySuggestDestinationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val suggestButton = findViewById<Button>(R.id.decide_suggestion_button)
        suggestButton.setOnClickListener {
            //ここで最終的なピンの情報をDBに保存(APIを叩く)
            if(suggestMarker != null)
                decidePin()

            val intent = Intent(this, ViewerActivity::class.java)
            startActivity(intent)
            finish()
        }

        val cancelButton = findViewById<Button>(R.id.cancel_suggestion_button)
        cancelButton.setOnClickListener {
            val intent = Intent(this, ViewerActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

        mMap.setOnMapClickListener(this)
    }

    override fun onMapClick(point: LatLng) {
        suggestDestination = point
        suggestMarker?.remove()
        suggestMarker = mMap.addMarker(
            MarkerOptions().position(point)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
    }

    private fun decidePin(){
        val latitude = suggestDestination.latitude
        val longitude = suggestDestination.longitude
        thread {
            try {
                // APIを実行
                val service: SuggestDestinationService =
                    retrofit.create(SuggestDestinationService::class.java)
                val suggestDestinationResponse = service.suggestDestination(
                    //userID仮置き!!!!
                    user_id = 6, lat = latitude, lon = longitude, suggestion_flag = 1
                ).execute().body()
                    ?: throw IllegalStateException("body is null")

                Handler(Looper.getMainLooper()).post {
                    // 実行結果を出力
                    Log.d("suggestDestinationResponse", suggestDestinationResponse.toString())
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    // エラー内容を出力
                    Log.e("error", e.message.toString())

                    // 通信に失敗したことを通知
                    val toast =
                        Toast.makeText(this, "通信に失敗しました", Toast.LENGTH_SHORT)
                    toast.show()
                }
            }
        }
    }
}