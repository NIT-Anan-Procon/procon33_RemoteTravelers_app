package com.example.procon33_remotetravelers_app.activities

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
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
import com.example.procon33_remotetravelers_app.models.apis.Comment
import com.example.procon33_remotetravelers_app.models.apis.FootPrints
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.example.procon33_remotetravelers_app.models.apis.GetInfoResponse
import com.example.procon33_remotetravelers_app.models.apis.GetUpdatedInfoResponse
import com.example.procon33_remotetravelers_app.services.GetInfoService
import com.example.procon33_remotetravelers_app.services.AddCommentService
import com.google.android.gms.maps.CameraUpdateFactory
import com.example.procon33_remotetravelers_app.services.GetUpdatedInfoService
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
        const val WC = LinearLayout.LayoutParams.WRAP_CONTENT
        const val MP = LinearLayout.LayoutParams.MATCH_PARENT

        var stopUpdateFlag = true
        var updateRequestFlag = false
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
    private lateinit var updatedInfo: GetUpdatedInfoResponse
    private lateinit var suggestLocation: LatLng
    private var markerTouchFrag: Boolean = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = intent.getIntExtra("userId", 0)
        //初回の画面表示
        thread {
            //画面情報を取得できるまで繰り返す
            while(!::info.isInitialized) {
                getInfo(userId)
                Thread.sleep(2500)
            }
            //マップ表示まで待機
            while(!::mMap.isInitialized){
                Thread.sleep(100)
            }
            Handler(Looper.getMainLooper()).post {
                //旅レポート生成
                DisplayReportActivity.createReportMarker(mMap, info.reports, visible = false)
            }
            //旅の追体験
            relive(mMap, info.route)
            Handler(Looper.getMainLooper()).post {
                //現在地表示
                CurrentLocationActivity.displayCurrentLocation(
                    mMap,
                    LatLng(info.current_location!!.lat, info.current_location!!.lon),
                )
                //行先提案ピン表示
                DisplayPinActivity.displayPin(mMap, info.destination)
                //コメント表示
                displayComment(info.comments)
                //旅行者の現在状況表示
                displaySituation(info.situation)
            }
            Thread.sleep(2500)
            stopUpdateFlag = false
        }
        //定期的に画面を更新
        Timer().scheduleAtFixedRate(0, 5000){
            if(!stopUpdateFlag) {
                //画面更新
                update(userId)
            }
        }
        //画面更新リクエストを待機
        Timer().scheduleAtFixedRate(0, 100){
            if(updateRequestFlag) {
                Thread.sleep(1000)
                //画面更新
                update(userId)
                updateRequestFlag = false
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
            if(!stopUpdateFlag) {
                val intent = Intent(this, SuggestDestinationActivity::class.java)
                intent.putExtra("userId", userId)
                intent.putExtra("lat", mMap.cameraPosition.target.latitude)
                intent.putExtra("lon", mMap.cameraPosition.target.longitude)
                intent.putExtra("zoom", mMap.cameraPosition.zoom)
                startActivity(intent)
            }
        }

        val currentLocationButton = findViewById<Button>(R.id.viewer_current_location_button)
        currentLocationButton.setOnClickListener {
            if(!stopUpdateFlag){
                val (text, color) = CurrentLocationActivity.pressedButton()
                currentLocationButton.setText(text)
                currentLocationButton.setBackgroundResource(color)
                CurrentLocationActivity.displayCurrentLocation(mMap, CurrentLocationActivity.currentLocation)
            }
        }

        // コメント欄の開け閉め
        var fragment = false
        val buttonComment = findViewById<Button>(R.id.comment_door_button)
        buttonComment.setOnClickListener {
            if(!stopUpdateFlag){
                fragment = !fragment
                //コメント表示切替
                moveComment(fragment)
            }
        }

        // コメントの送信
        val submitComment = findViewById<Button>(R.id.comment_submit)
        submitComment.setOnClickListener {
            if(!stopUpdateFlag) {
                val comment = findViewById<EditText>(R.id.comment_text)
                addComment(userId, comment.text.toString())
                comment.setText("")
            }
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
    //マップを初期化
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        CurrentLocationActivity.initializeMap(mMap)
        mMap.setInfoWindowAdapter(CustomInfoWindow(this))
        mMap.setOnInfoWindowClickListener(this)
        mMap.setOnInfoWindowCloseListener(CustomInfoWindow(this))
        mMap.setOnMarkerClickListener(this)
    }

    //マーカーがクリックされたとき
    override fun onMarkerClick(marker: Marker): Boolean {
        if(!DisplayReportActivity.markers.contains(marker)) {
            //ルート処理
            suggestLocation = LatLng(marker.position.latitude, marker.position.longitude)
            markerTouchFrag = !markerTouchFrag
            if (markerTouchFrag) {
                DisplayPinActivity.displayRoute(
                    mMap,
                    LatLng(info.current_location!!.lat, info.current_location!!.lon),
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

    //旅レポートがクリックされたとき
    override fun onInfoWindowClick(marker: Marker) {
        val intent = Intent(this, ViewReportActivity::class.java)
        intent.putExtra("index", DisplayReportActivity.markers.indexOf(marker))
        intent.putExtra("isRelive", false)
        startActivity(intent)
    }

    //全ての画面情報を取得
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
                if(getInfoResponse.situation == null){
                    getInfoResponse.situation = "移動中"
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

    @RequiresApi(Build.VERSION_CODES.O)
    //更新された情報を取得・画面更新
    private fun update(userId :Int){
        //更新された情報を取得
        getUpdatedInfo(userId)
        Thread.sleep(300)
        //取得できていないとき
        if(!::updatedInfo.isInitialized) {
            return
        }
        Handler(Looper.getMainLooper()).post {
            //現在地の更新があるか
            if (updatedInfo.current_location != null) {
                //現在地を表示
                CurrentLocationActivity.displayCurrentLocation(
                    mMap,
                    LatLng(
                        updatedInfo.current_location!!.lat,
                        updatedInfo.current_location!!.lon,
                    )
                )
                //旅行者が通ったルートを表示
                DrawRouteActivity.drawRoute(
                    mMap,
                    LatLng(
                        updatedInfo.current_location!!.lat,
                        updatedInfo.current_location!!.lon,
                    )
                )
                //行き先提案までのルート表示中のとき
                if (markerTouchFrag) {
                    //旅行者の現在位置に合わせたルートを提案
                    DisplayPinActivity.displayRoute(
                        mMap,
                        LatLng(
                            updatedInfo.current_location!!.lat,
                            updatedInfo.current_location!!.lon,
                        ),
                        suggestLocation,
                    )
                }
            }
            //行き先提案に更新があるか
            if (updatedInfo.destination != null) {
                //行先提案を再表示
                DisplayPinActivity.displayPin(mMap, updatedInfo.destination!!)
            }
            //コメントに更新があるか
            if (updatedInfo.comments != null) {
                //コメントを再表示
                displayComment(updatedInfo.comments!!)
            }
            //現在情報に更新があるか
            if (updatedInfo.situation != null) {
                //現在情報を再表示
                displaySituation(updatedInfo.situation!!)
            }
            //旅レポートに更新があるか
            if(updatedInfo.reports != null){
                //旅レポートを再表示
                DisplayReportActivity.createReportMarker(mMap, updatedInfo.reports!!, visible = true)
            }
        }
    }

    //更新情報を取得
    private fun getUpdatedInfo(userId: Int){
        thread {
            try {
                // APIを実行
                val service: GetUpdatedInfoService =
                    retrofit.create(GetUpdatedInfoService::class.java)
                val getUpdatedInfoResponse = service.getUpdatedInfo(
                    user_id = userId
                ).execute().body()
                    ?: throw IllegalStateException("body is null")

                Handler(Looper.getMainLooper()).post {
                    // 実行結果を出力
                    Log.d("getUpdatedInfoResponse", getUpdatedInfoResponse.toString())
                }
                updatedInfo = getUpdatedInfoResponse
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    // エラー内容を出力
                    Log.e("error", e.message.toString())
                }
            }
        }
    }

    // 状況把握の画像・テキストを変更
    private fun displaySituation(situation: String?){
        val travelerText = findViewById<TextView>(R.id.traveler_situation_text)
        val travelerIcon = findViewById<ImageView>(R.id.traveler_situation_icon)
        travelerText.text = situation ?: "移動中"
        travelerIcon.setImageResource (
            when(situation){
                "食事中" -> R.drawable.eatting
                "観光中(建物)" -> R.drawable.building
                "観光中(風景)" -> R.drawable.nature
                "動物に癒され中" -> R.drawable.animal
                "人と交流中" -> R.drawable.human
                else -> R.drawable.walking
            }
        )
    }

    //コメント画面を表示・非表示
    private fun moveComment(fragment: Boolean) {
        val commentList: View = findViewById(R.id.comments) // 対象となるオブジェクト
        val commentBottom = findViewById<Button>(R.id.comment_door_button)
        val destination = if (fragment) -1230f else 0f
        ObjectAnimator.ofFloat(commentList, "translationY", destination).apply {
            duration = 200 // ミリ秒
            start() // アニメーション開始
        }
        commentBottom.text =
            if (fragment) {
                "コメントを閉じる"
            } else {
                "コメントを開く"
            }
    }

    //コメントのテキストを表示
    private fun displayComment(comments: List<Comment?>){
        try {
            val travelerCommentColor = "#FFA800"    //オレンジ
            val viewerCommentColor = "#4B4B4B"      //白
            val commentList = findViewById<LinearLayout>(R.id.comment_list)
            commentList.removeAllViews()
            for (comment in comments) {
                val commentText = comment!!.comment
                val commentColor =
                    when(comment.traveler){
                        1 -> travelerCommentColor
                        else -> viewerCommentColor
                    }
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

    //コメントを投稿
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
                updateRequestFlag = true
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    // エラー内容を出力
                    Log.e("error", e.message.toString())
                }
            }
        }
    }

    //旅の追体験
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
                    15f,
                )
            )
        }
        Thread.sleep(2500)
        for (route in routes) {
            Thread.sleep(20)
            val location = LatLng(route!!.lat, route.lon)
            Handler(Looper.getMainLooper()).post {
                DrawRouteActivity.drawRoute(mMap, location)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
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
                Handler(Looper.getMainLooper()).post {
                    marker.hideInfoWindow()
                    marker.alpha = 1f
                }
                Thread.sleep(300)
            }
        }
    }
}
