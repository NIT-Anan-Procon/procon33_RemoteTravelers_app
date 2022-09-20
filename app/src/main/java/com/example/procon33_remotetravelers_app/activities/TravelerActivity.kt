package com.example.procon33_remotetravelers_app.activities

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.content.pm.PackageManager
import android.graphics.Color
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
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.procon33_remotetravelers_app.BuildConfig
import com.example.procon33_remotetravelers_app.R
import com.example.procon33_remotetravelers_app.databinding.ActivityTravelerBinding
import com.example.procon33_remotetravelers_app.models.apis.DisplayPinActivity
import com.example.procon33_remotetravelers_app.models.apis.GetInfoResponse
import com.example.procon33_remotetravelers_app.services.AddCommentService
import com.example.procon33_remotetravelers_app.services.GetInfoService
import com.example.procon33_remotetravelers_app.services.SaveCurrentLocationService
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.concurrent.thread
import kotlin.properties.Delegates


class TravelerActivity : AppCompatActivity(), OnMapReadyCallback,
    LocationListener {

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
    private lateinit var info: GetInfoResponse
    private lateinit var locationManager: LocationManager
    private lateinit var currentLocation: LatLng
    private var userId by Delegates.notNull<Int>()
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
        thread {
            Thread.sleep(2500)
            getInfo(userId)
            Handler(Looper.getMainLooper()).post {
                if (::mMap.isInitialized && ::info.isInitialized) {
                    CurrentLocationActivity.displayCurrentLocation(mMap, LatLng(info.current_location.lat, info.current_location.lon))
                }
            }
        }
        Timer().scheduleAtFixedRate(0, 5000){
            getInfo(userId)
            Handler(Looper.getMainLooper()).post {
                if (::mMap.isInitialized && ::info.isInitialized) {
                    CurrentLocationActivity.displayCurrentLocation(mMap, LatLng(info.current_location.lat, info.current_location.lon))
                    DrawRoot.drawRoot(mMap, LatLng(info.current_location.lat, info.current_location.lon))
                    DisplayPinActivity.displayPin(mMap, info.destination)
                }
                displayComment()
            }
        }

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

        val currentLocationButton = findViewById<Button>(R.id.travel_current_location_button)
        currentLocationButton.setOnClickListener {
            if(::mMap.isInitialized && ::currentLocation.isInitialized){
                val (text, color) = CurrentLocationActivity.pressedButton()
                currentLocationButton.setText(text)
                currentLocationButton.setBackgroundResource(color)
                CurrentLocationActivity.displayCurrentLocation(mMap, currentLocation)
            }
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            locationStart()
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
            val commentText = comment.text.toString()
            if (commentText != "") addComment(userId, commentText)
            comment.setText("")
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
            CurrentLocationActivity.displayCurrentLocation(mMap, currentLocation)
            DrawRoot.drawRoot(mMap, currentLocation)
        }
    }

    override fun onProviderEnabled(provider: String) {

    }

    override fun onProviderDisabled(provider: String) {

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        CurrentLocationActivity.initializeMap(mMap)
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

    private fun moveComment(fragment: Boolean) {
        // コメントを取得
        val target: View = findViewById(R.id.comments) // 対象となるオブジェクト
        val destination = if (fragment) -1100f else 0f
        ObjectAnimator.ofFloat(target, "translationY", destination).apply {
            duration = 200 // ミリ秒
            start() // アニメーション開始
        }
    }

    private fun displayComment(){
        try {
            val commentList = findViewById<LinearLayout>(R.id.comment_list)
            commentList.removeAllViews()
            val WC = LinearLayout.LayoutParams.WRAP_CONTENT
            val MP = LinearLayout.LayoutParams.MATCH_PARENT
            // 最初のコメントが見えないのでダミーコメント
            commentList.addView(setView("↑コメントが表示されます↑", "#D4D4D4"), 0, LinearLayout.LayoutParams(MP, WC))
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
}