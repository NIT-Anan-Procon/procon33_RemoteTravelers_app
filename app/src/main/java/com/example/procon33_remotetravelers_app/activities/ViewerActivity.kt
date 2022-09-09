package com.example.procon33_remotetravelers_app.activities

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.procon33_remotetravelers_app.BuildConfig
import com.example.procon33_remotetravelers_app.R
import com.example.procon33_remotetravelers_app.databinding.ActivityViewerBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.procon33_remotetravelers_app.models.apis.GetInfoResponse
import com.example.procon33_remotetravelers_app.services.GetInfoService
import com.example.procon33_remotetravelers_app.services.AddCommentService
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PolylineOptions
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
    private var track = false
    private var firstTrack = true
    private var setUpped = true
    private var currentLocationMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lastLocation = LatLng(0.0, 0.0)
        val userId = intent.getIntExtra("userId", 0)
        thread {
            Thread.sleep(2500)
            getInfo(userId)
            Handler(Looper.getMainLooper()).post {
                if (::info.isInitialized && ::mMap.isInitialized) {
                    displayCurrentLocation()
                }
            }
        }
        Timer().scheduleAtFixedRate(0, 5000){
            getInfo(userId)
            Handler(Looper.getMainLooper()).post {
                if (::info.isInitialized && ::mMap.isInitialized) {
                    displayCurrentLocation()
                    drawRoot()
                }
            }
        }

        binding = ActivityViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val pinButton = findViewById<Button>(R.id.pin_button)
        pinButton.setOnClickListener {
            val intent = Intent(this, SuggestDestinationActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        val currentLocationButton = findViewById<Button>(R.id.viewer_current_location_button)
        currentLocationButton.setOnClickListener {
            track = !track
            firstTrack = track
            val text: Int
            val color: Int
            when(track){
                true -> {
                    text = R.string.track_on
                    color = R.drawable.track_on
                }
                else -> {
                    text = R.string.track_off
                    color = R.drawable.track_off
                }
            }
            currentLocationButton.setText(text)
            currentLocationButton.setBackgroundResource(color)
            displayCurrentLocation()
        }

        var fragment = false
        val buttonComment = findViewById<Button>(R.id.comment_door_button)
        buttonComment.setOnClickListener {
            fragment = !fragment
            moveComment(fragment)
        }

        val submitComment = findViewById<Button>(R.id.comment_submit)
        submitComment.setOnClickListener {
            val comment = findViewById<EditText>(R.id.comment_text)
            addComment(userId, comment.text.toString())
            comment.setText("")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val tokyo = LatLng(35.90684931, 139.68896404)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(tokyo.latitude, 180 - tokyo.longitude)))
        thread {
            Thread.sleep(300)
            Handler(Looper.getMainLooper()).post {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(tokyo, 4f))
            }
        }
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

    private fun displayCurrentLocation() {
        val currentLocation = LatLng(info.current_location.lat, info.current_location.lon)
        if (lastLocation != currentLocation) {
            currentLocationMarker?.remove()
            currentLocationMarker =
                mMap.addMarker(MarkerOptions().position(currentLocation).title("現在地"))
            if (track) {
                mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLocation))
            }
            lastLocation = currentLocation
        }
        if (firstTrack) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
            if (setUpped) {
                thread {
                    Thread.sleep(2000)
                    Handler(Looper.getMainLooper()).post {
                        mMap.setMinZoomPreference(7f)
                    }
                }
                setUpped = false
            }
            firstTrack = false
        }
    }

    private fun moveComment(fragment: Boolean) {
        val target: View = findViewById(R.id.comments) // 対象となるオブジェクト
        val destination = if (fragment) -550f else 0f
        ObjectAnimator.ofFloat(target, "translationY", destination).apply {
            duration = 200 // ミリ秒
            start() // アニメーション開始
        }
    }

    private fun addComment(userId: Int, comment: String) {
        thread {
            try {
                // APIを実行
                val service: AddCommentService =
                    retrofit.create(AddCommentService::class.java)
                val addCommentResponse = service.addComment(
                    user_id = userId, comment = comment
                ).execute().body()
                    ?: throw IllegalStateException("body is null")

                Handler(Looper.getMainLooper()).post {
                    // 実行結果を出力
                    Log.d("addCommentResponse", addCommentResponse.toString())
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    // エラー内容を出力
                    Log.e("error", e.message.toString())
                }
            }
        }
    }

    // 線の太さを10pxに設定
    private val INITIAL_STROKE_WIDTH_PX = 10

    private fun drawRoot(){
        val currentLatLng = LatLng(info.current_location.lat, info.current_location.lon)
        val beforeLocation = info.route[info.route.size - 2] ?: return
        val beforeLatLng = LatLng(beforeLocation.lat, beforeLocation.lon)
        mMap.addPolyline(
        PolylineOptions()
            .add(beforeLatLng, currentLatLng)
            .width(INITIAL_STROKE_WIDTH_PX.toFloat()).color(Color.parseColor("#766BF3FF")).geodesic(true)
        )
    }
}
