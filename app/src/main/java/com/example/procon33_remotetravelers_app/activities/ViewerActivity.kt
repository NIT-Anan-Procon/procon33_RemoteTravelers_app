package com.example.procon33_remotetravelers_app.activities

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.procon33_remotetravelers_app.BuildConfig
import com.example.procon33_remotetravelers_app.R
import com.example.procon33_remotetravelers_app.databinding.ActivityViewerBinding
import com.example.procon33_remotetravelers_app.models.apis.FootPrints
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.example.procon33_remotetravelers_app.models.apis.GetInfoResponse
import com.example.procon33_remotetravelers_app.services.GetInfoService
import com.example.procon33_remotetravelers_app.services.AddCommentService
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.Marker
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*
import kotlin.concurrent.thread
import kotlin.concurrent.scheduleAtFixedRate
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener

class ViewerActivity : AppCompatActivity(), OnMapReadyCallback,
    OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener {
    companion object{
        var stopRelive = true
    }

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
    private lateinit var suggestLocation: LatLng
    private var markerTouchFrag: Boolean = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = intent.getIntExtra("userId", 0)
        thread {
            Thread.sleep(2500)
            getInfo(userId)
            if (::mMap.isInitialized && ::info.isInitialized) {
                Handler(Looper.getMainLooper()).post {
                    DisplayReportActivity.createReportMarker(mMap, info.reports)
                }
                relive(mMap, info.route)
                Handler(Looper.getMainLooper()).post {
                    CurrentLocationActivity.displayCurrentLocation(
                        mMap,
                        LatLng(info.current_location.lat, info.current_location.lon)
                    )
                    DisplayPinActivity.displayPin(mMap, info.destination)
                }
            }
        }
        Timer().scheduleAtFixedRate(0, 5000){
            getInfo(userId)
            Handler(Looper.getMainLooper()).post {
                if (::mMap.isInitialized && ::info.isInitialized) {
                    CurrentLocationActivity.displayCurrentLocation(mMap, LatLng(info.current_location.lat, info.current_location.lon))
                    DisplayPinActivity.displayPin(mMap, info.destination)
                    DrawRoute.drawRoute(mMap, LatLng(info.current_location.lat, info.current_location.lon))
                    if(markerTouchFrag){
                        DisplayPinActivity.displayRoute(mMap, LatLng(info.current_location.lat, info.current_location.lon), suggestLocation)
                    }else {
                        DisplayPinActivity.clearRoute()
                    }
                }
                displayComment()
                changeSituation()
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
            intent.putExtra("lat", mMap.cameraPosition.target.latitude)
            intent.putExtra("lon", mMap.cameraPosition.target.longitude)
            intent.putExtra("zoom", mMap.cameraPosition.zoom)
            startActivity(intent)
        }

        val currentLocationButton = findViewById<Button>(R.id.viewer_current_location_button)
        currentLocationButton.setOnClickListener {
            if (::mMap.isInitialized && ::info.isInitialized) {
                val (text, color) = CurrentLocationActivity.pressedButton()
                currentLocationButton.setText(text)
                currentLocationButton.setBackgroundResource(color)
                CurrentLocationActivity.displayCurrentLocation(mMap, LatLng(info.current_location.lat, info.current_location.lon))
            }
        }

        // コメント欄の開け閉め
        var fragment = false
        val buttonComment = findViewById<Button>(R.id.comment_door_button)
        buttonComment.setOnClickListener {
            fragment = !fragment
            moveComment(fragment)
        }

        // コメントの送信
        val submitComment = findViewById<Button>(R.id.comment_submit)
        submitComment.setOnClickListener {
            val comment = findViewById<EditText>(R.id.comment_text)
            addComment(userId, comment.text.toString())
            comment.setText("")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        CurrentLocationActivity.initializeMap(mMap)
        mMap.setInfoWindowAdapter(CustomInfoWindow(this))
        mMap.setOnInfoWindowClickListener(this)
        mMap.setOnInfoWindowCloseListener(CustomInfoWindow(this))
        mMap.setOnMarkerClickListener(this)
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        if(!DisplayReportActivity.markers.contains(marker)) {
            //ルート処理
            suggestLocation = LatLng(marker.position.latitude, marker.position.longitude)
            markerTouchFrag = !markerTouchFrag
            if (markerTouchFrag) {
                DisplayPinActivity.displayRoute(
                    mMap,
                    LatLng(info.current_location.lat, info.current_location.lon),
                    suggestLocation
                )
                return true
            }
            DisplayPinActivity.clearRoute()
            return true
        }
        //マーカーを透明に設定
        marker.alpha = 0f
        marker.showInfoWindow()
        return true
    }

    override fun onInfoWindowClick(marker: Marker) {
        val intent = Intent(this, ViewReportActivity::class.java)
        intent.putExtra("index", DisplayReportActivity.markers.indexOf(marker))
        intent.putExtra("isRelive", false)
        startActivity(intent)
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

    // 状況把握の画像・テキストを変更
    private fun changeSituation(){
        try {
            val travelerText = findViewById<TextView>(R.id.traveler_situation_text)
            val travelerIcon = findViewById<ImageView>(R.id.traveler_situation_icon)
            travelerText.text= info.situation
            travelerIcon.setImageResource (
                when(info.situation){
                    "食事中" -> R.drawable.eatting
                    "観光中(建物)" -> R.drawable.building
                    "観光中(風景)" -> R.drawable.nature
                    "動物に癒され中" -> R.drawable.animal
                    "人と交流中" -> R.drawable.human
                    else -> R.drawable.walking
                }
            )
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                // エラー内容を出力
                Log.e("situation_error", e.message.toString())
            }
        }
    }

    private fun moveComment(fragment: Boolean) {
        val commentList: View = findViewById(R.id.comments) // 対象となるオブジェクト
        val commentBottom = findViewById<Button>(R.id.comment_door_button)
        val destination = if (fragment) -1230f else 0f
        ObjectAnimator.ofFloat(commentList, "translationY", destination).apply {
            duration = 200 // ミリ秒
            start() // アニメーション開始
        }
        commentBottom.text = if (fragment) "コメントを閉じる" else "コメントを開く"
    }

    private fun displayComment(){
        try {
            val commentList = findViewById<LinearLayout>(R.id.comment_list)
            commentList.removeAllViews()
            val WC = LinearLayout.LayoutParams.WRAP_CONTENT
            val MP = LinearLayout.LayoutParams.MATCH_PARENT
            for (oneComment in info.comments) {
                if (oneComment == null) {
                    Log.d("oneComment", "null")
                    continue
                }
                val commentText: String = oneComment.comment
                val commentColor: String = if(oneComment.traveler == 0) "#FFA800" else "#4B4B4B"
                commentList.addView(setView(commentText, commentColor), 0, LinearLayout.LayoutParams(MP, WC))
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                // エラー内容を出力
                Log.e("getCommentError", e.message.toString())
            }
        }
    }

    // コメントのviewを設定する関数
    private fun setView (commentText: String, commentColor: String): TextView{
        val comment = TextView(this)
        comment.text = commentText
        comment.textSize = 28f
        comment.setTextColor(Color.parseColor(commentColor))
        comment.setPadding(10, 15, 10, 15)
        comment.setBackgroundResource(R.drawable.comment_design)
        return comment
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

    private fun relive(mMap: GoogleMap, routes: List<FootPrints?>){
        if(routes.isEmpty()){
            return
        }
        var index = 0
        Handler(Looper.getMainLooper()).post {
            mMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        routes[0]!!.lat,
                        routes[0]!!.lon
                    ),
                    10f,
                )
            )
        }
        Thread.sleep(2500)
        for (route in routes) {
            Thread.sleep(200)
            val location = LatLng(route!!.lat, route.lon)
            Handler(Looper.getMainLooper()).post {
                DrawRoute.drawRoute(mMap, location)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 20f))
            }
            if (route.flag == 1) {
                Thread.sleep(500)
                val marker = DisplayReportActivity.markers[index++]
                Handler(Looper.getMainLooper()).post {
                    marker.showInfoWindow()
                }
                Thread.sleep(1500)
                val intent = Intent(this, ViewReportActivity::class.java)
                intent.putExtra("index", DisplayReportActivity.markers.indexOf(marker))
                intent.putExtra("isRelive", true)
                startActivity(intent)
                stopRelive = true
                while(stopRelive){
                    Thread.sleep(500)
                }
                Thread.sleep(300)
            }
        }
    }
}
