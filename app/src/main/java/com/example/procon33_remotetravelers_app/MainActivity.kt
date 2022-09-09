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
import com.example.procon33_remotetravelers_app.activities.ViewerActivity
import com.example.procon33_remotetravelers_app.services.CheckTravellingService
import com.example.procon33_remotetravelers_app.services.SignupService
import com.example.procon33_remotetravelers_app.services.StartTravelService
import com.example.procon33_remotetravelers_app.services.SuggestDestinationService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.sql.Timestamp
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ユーザIDを取得し表示する
        var userId = getUserId()
        val userIdText = findViewById<TextView>(R.id.userIdText)
        if (userId.isNotEmpty()) {
            userId = getString(R.string.user_id_text, userId)
            userIdText.text = userId
        } else {
            signup(userIdText)
        }

        checkTravelling()

        // 旅行するボタンが押されるとTravelerActivityに遷移する
        val travelButton = findViewById<Button>(R.id.travel_button)
        travelButton.setOnClickListener {
            startTravel()
            val intent = Intent(this, TravelerActivity::class.java)
            intent.putExtra("userId", getUserId().toInt())
            startActivity(intent)
        }

        // デバッグ用ボタンが押されるとViewerActivityに遷移する
        val debugButton = findViewById<Button>(R.id.debug_button)
        debugButton.setOnClickListener {
            val intent = Intent(this, ViewerActivity::class.java)
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

    private fun checkTravelling(){
        val userId = getUserId().toInt()
        val lastRequest: Timestamp = Timestamp(0)

        //旅行をしているか、または旅行に参加しているかを判断するAPIを叩く
        thread {
            try {
                val service: CheckTravellingService =
                    retrofit.create(CheckTravellingService::class.java)
                val checkTravellingService = service.checkTravelling(
                    user_id = userId, last_request = lastRequest
                ).execute().body()
                    ?: throw IllegalStateException("body is null")

                Handler(Looper.getMainLooper()).post {
                    // 実行結果を出力
                    Log.d("suggestDestinationResponse", checkTravellingService.toString())
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
