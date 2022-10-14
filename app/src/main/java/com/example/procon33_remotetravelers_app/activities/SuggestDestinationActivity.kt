package com.example.procon33_remotetravelers_app.activities

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
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate
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
    private var currentLocationMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timer().scheduleAtFixedRate(0, 5000) {
            if(::mMap.isInitialized) {
                Handler(Looper.getMainLooper()).post {
                    displayCurrentLocation()
                }
            }
        }

        val userId = intent.getIntExtra("userId", 0)

        binding = ActivitySuggestDestinationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //行先提案を確定
        val suggestButton = findViewById<Button>(R.id.decide_suggestion_button)
        suggestButton.setOnClickListener {
            //ここで最終的なピンの情報をDBに保存(APIを叩く)
            if(suggestMarker != null) {
                decidePin(userId)
            }
            //閲覧者画面に戻る
            finish()
        }

        //行先提案をやめる
        val cancelButton = findViewById<Button>(R.id.cancel_suggestion_button)
        cancelButton.setOnClickListener {
            //閲覧者画面に戻る
            finish()
        }
    }

    //マップ起動時
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val cameraPosition = LatLng(intent.getDoubleExtra("lat", 0.0), intent.getDoubleExtra("lon", 0.0))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cameraPosition, intent.getFloatExtra("zoom", 15f)))
        mMap.setMinZoomPreference(7f)
        displayCurrentLocation()
        mMap.setOnMapClickListener(this)
    }

    //マップがクリックされたとき
    override fun onMapClick(point: LatLng) {
        suggestDestination = point
        suggestMarker?.remove()
        //行先提案ピンを仮置き
        suggestMarker = mMap.addMarker(
            MarkerOptions().position(point)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
    }

    //現在地ピンを表示
    private fun displayCurrentLocation(){
        currentLocationMarker?.remove()
        currentLocationMarker =
            mMap.addMarker(MarkerOptions().position(ViewerActivity.currentLocation).title("現在地"))
    }

    //行先提案ピンを確定しDBに保存
    private fun decidePin(userId: Int){
        val latitude = suggestDestination.latitude
        val longitude = suggestDestination.longitude
        thread {
            try {
                // APIを実行
                val service: SuggestDestinationService =
                    retrofit.create(SuggestDestinationService::class.java)
                val suggestDestinationResponse = service.suggestDestination(
                    user_id = userId, lat = latitude, lon = longitude, suggestion_flag = 1
                ).execute().body()
                    ?: throw IllegalStateException("body is null")

                Handler(Looper.getMainLooper()).post {
                    // 実行結果を出力
                    Log.d("suggestDestinationResponse", suggestDestinationResponse.toString())
                }
                ViewerActivity.updateRequestFlag = true
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