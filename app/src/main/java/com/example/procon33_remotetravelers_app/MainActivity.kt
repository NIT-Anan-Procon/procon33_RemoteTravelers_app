package com.example.procon33_remotetravelers_app

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.procon33_remotetravelers_app.activities.TravelerActivity
import com.example.procon33_remotetravelers_app.activities.ViewerActivity
import com.example.procon33_remotetravelers_app.services.SignupService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // SharePreferencesからユーザIDを取得
        val pref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        var userId = pref.getString("userId", "")

        // ユーザIDが登録されていれば表示し、無ければ新規登録APIをたたき表示する
        val userIdText = findViewById<TextView>(R.id.userIdText)
        if (userId!!.isNotEmpty()) {
            userId = getString(R.string.user_id_text, userId)
            userIdText.text = userId
        } else {
            getUserId(userIdText)
        }

        // 旅行するボタンが押されるとTravelerActivityに遷移する
        val travelButton = findViewById<Button>(R.id.travel_button)
        travelButton.setOnClickListener {
            val intent = Intent(this, TravelerActivity::class.java)
            startActivity(intent)
        }

        // デバッグ用ボタンが押されるとViewerActivityに遷移する
        val debugButton = findViewById<Button>(R.id.debug_button)
        debugButton.setOnClickListener {
            val intent = Intent(this, ViewerActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getUserId(userIdText: TextView) {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        thread {
            try {
                 val service: SignupService =
                     retrofit.create(SignupService::class.java)
                val signupApiResponse = service.getUserId().execute().body()
                    ?: throw IllegalStateException("body is null")

                Handler(Looper.getMainLooper()).post {
                    val userId = signupApiResponse.data.toString()
                    getSharedPreferences("AppSettings", Context.MODE_PRIVATE).edit().apply {
                        putString("userId", userId)
                        apply()
                    }

                    userIdText.text = getString(R.string.user_id_text, userId)
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    Log.e("error", e.message.toString())
                    val toast =
                        Toast.makeText(this, "新規登録に失敗しました", Toast.LENGTH_SHORT)
                    toast.show()
                }
            }
        }
    }
}
