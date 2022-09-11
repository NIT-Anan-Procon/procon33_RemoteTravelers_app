package com.example.procon33_remotetravelers_app.activities
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
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
        thread {
            Thread.sleep(2000)
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
            getComment()
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

    private fun displayCurrentLocation(){
        val currentLocation = LatLng(info.current_location.lat, info.current_location.lon)
        if(lastLocation == currentLocation)
            return
        lastLocation = currentLocation
        currentLocationMarker?.remove()
        currentLocationMarker = mMap.addMarker(MarkerOptions().position(currentLocation).title("現在地"))
        if(firstTrack) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(35.90684931, 139.68896404), 4f))
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
            thread {
                Thread.sleep(2000)
                Handler(Looper.getMainLooper()).post {
                    mMap.setMinZoomPreference(7f)
                }
            }
            return
        }
        if(track)
            mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLocation))
    }

    private fun moveComment(fragment: Boolean) {
        // コメントを取得
        val target: View = findViewById(R.id.comments) // 対象となるオブジェクト
        val destination = if (fragment) -1100f else 0f
        if (fragment) getComment()
        ObjectAnimator.ofFloat(target, "translationY", destination).apply {
            duration = 200 // ミリ秒
            start() // アニメーション開始
        }
    }

    private fun getComment(){
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
                val textView = TextView(this)
                val commentText: String = oneComment.comment
                textView.text = commentText
                textView.textSize = 28f
                textView.setPadding(10, 15, 10, 15)
                textView.setBackgroundResource(R.drawable.comment_design)
                commentList.addView(textView, 0, LinearLayout.LayoutParams(MP, WC))
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                // エラー内容を出力
                Log.e("error", e.message.toString())
            }
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
}
