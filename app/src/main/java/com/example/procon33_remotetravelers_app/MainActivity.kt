package com.example.procon33_remotetravelers_app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.procon33_remotetravelers_app.activities.TravelerActivity
import com.example.procon33_remotetravelers_app.activities.ViewAlbumActivity
import com.example.procon33_remotetravelers_app.activities.ViewerActivity
import com.example.procon33_remotetravelers_app.services.CheckTravelingService
import com.example.procon33_remotetravelers_app.services.SignupService
import com.example.procon33_remotetravelers_app.services.StartTravelService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val timer = Timer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ユーザIDを取得し表示する
        var userId = getUserId()
        val userIdText = findViewById<TextView>(R.id.userIdText)
        if (userId.isNotEmpty()) {
            userId = getString(R.string.user_id_text, userId)
            userIdText.text = userId
            timer.scheduleAtFixedRate(0, 2000){
                //すでに旅行に参加しているかどうかの判断
                checkTraveling()
            }
        } else {
            signup(userIdText)
        }

        // 旅行するボタンが押されるとTravelerActivityに遷移する
        val travelButton = findViewById<Button>(R.id.travel_button)
        travelButton.setOnClickListener {
            startTravel()

            //自動参加と重複しないようにtimerをキャンセル
            timer.cancel()
            startTravel()
            val intent = Intent(this, TravelerActivity::class.java)
            intent.putExtra("userId", getUserId().toInt())
            startActivity(intent)
            finish()
        }

        //旅アルバムの画面に遷移
        val albumButton = findViewById<Button>(R.id.album_button)
        albumButton.setOnClickListener {
            val intent = Intent(this, ViewAlbumActivity::class.java)
            intent.putExtra("userId", getUserId().toInt())
            startActivity(intent)
        }
    }

    private fun signup(userIdText: TextView) {
        thread {
            try {
                // APIを実行
                val service: SignupService =
                     retrofit.create(SignupService::class.java)
                val signupApiResponse = service.getUserId().execute().body()
                    ?: throw IllegalStateException("body is null")

                Handler(Looper.getMainLooper()).post {
                    // レスポンスからuserIDを取得
                    val userId = signupApiResponse.data.toString()

                    // sharedPreferencesにuserIDを保存
                    getSharedPreferences("AppSettings", Context.MODE_PRIVATE).edit().apply {
                        putString("userId", userId)
                        apply()
                    }

                    // userIDを表示
                    userIdText.text = getString(R.string.user_id_text, userId)
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    // エラー内容を出力
                    Log.e("error", e.message.toString())

                    // 通信に失敗したことを通知する
                    val toast =
                        Toast.makeText(this, "新規登録に失敗しました", Toast.LENGTH_SHORT)
                    toast.show()
                }
            }
        }
    }

    private fun startTravel() {
        thread {
            try {
                // 旅行に参加するユーザのIDを取得
                val host = getUserId().toInt()
                val viewer1 = getViewerId(findViewById(R.id.viewer1))
                val viewer2 = getViewerId(findViewById(R.id.viewer2))
                val viewer3 = getViewerId(findViewById(R.id.viewer3))

                // APIを実行
                val service: StartTravelService =
                    retrofit.create(StartTravelService::class.java)
                val startTravelResponse = service.startTravel(
                    host, viewer1, viewer2, viewer3
                ).execute().body()
                    ?: throw IllegalStateException("body is null")

                Handler(Looper.getMainLooper()).post {
                    // 実行結果を出力
                    Log.d("startTravelResponse", startTravelResponse.toString())
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    // エラー内容を出力
                    Log.e("error", e.message.toString())

                    // 通信に失敗したことを通知
                    val toast =
                        Toast.makeText(this, "旅行を始めることができませんでした", Toast.LENGTH_SHORT)
                    toast.show()
                }
            }
        }
    }

    private fun getViewerId(viewer: EditText): Int {
        // 閲覧者のユーザIDが入力されていなければ0を返す
        val viewerId = viewer.text.toString()
        if (viewerId.isNotEmpty()) {
            return viewerId.toInt()
        } else {
            return 0
        }
    }

    private fun getUserId(): String {
        // SharePreferencesからユーザIDを取得
        val pref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val userId = pref.getString("userId", "").toString()
        return userId
    }

    private fun checkTraveling(){
        val userId = getUserId().toInt()
        //旅行をしているか、または旅行に参加しているかを判断するAPIを叩く
        try {
            val service: CheckTravelingService =
                retrofit.create(CheckTravelingService::class.java)
            val checkTraveling = service.checkTraveling(
                user_id = userId
            ).execute().body()
                ?: throw IllegalStateException("body is null")

            Handler(Looper.getMainLooper()).post {
                // 実行結果を出力
                Log.d("checkTravellingResponse", checkTraveling.toString())
            }

            //旅行者ならTravelerActivityに遷移
            if(checkTraveling.traveling == true){
                timer.cancel()
                if(checkTraveling.traveler == true){
                    val intent = Intent(this, TravelerActivity::class.java)
                    intent.putExtra("userId", getUserId().toInt())
                    startActivity(intent)
                    finish()
                } else {
                    val intent = Intent(this, ViewerActivity::class.java)
                    intent.putExtra("userId", getUserId().toInt())
                    startActivity(intent)
                    finish()
                }
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
