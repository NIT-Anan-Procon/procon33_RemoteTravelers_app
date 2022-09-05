package com.example.procon33_remotetravelers_app.activities

import android.content.Context
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.example.procon33_remotetravelers_app.BuildConfig
import com.example.procon33_remotetravelers_app.R
import com.example.procon33_remotetravelers_app.services.CreateReportService
import com.example.procon33_remotetravelers_app.services.SuggestDestinationService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlin.concurrent.thread

class CreateReportActivity : AppCompatActivity() {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private  val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_report)

        // TravelerActivityから写真データ(Bitmap)を取得する
        val intent = intent
        val photo = intent.getParcelableExtra<Bitmap>("data")

        // 受け取った写真データを表示
        val imageView = findViewById<ImageView>(R.id.report_image)
        imageView.setImageBitmap(photo)

        //ユーザーIDを取得
        val userId = getUserId().toInt()

        val keepButton = findViewById<Button>(R.id.keep_button)
        val backButton = findViewById<Button>(R.id.back_button)

        keepButton.setOnClickListener {
            saveData(userId, photo)
            finish()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    //DBにレポートの内容を保存する(ネストが深くなりそうだったので関数にする)
    private fun saveData(userId: Int, photo: Bitmap?){
        thread {
            try {
                val service: CreateReportService =
                    retrofit.create(CreateReportService::class.java)
                val createReportResponse = service.createReport(
                    user_id = userId, image = photo, comment = "a", excitement = 1, lat = 1.0, lon = 1.0
                ).execute().body() ?: throw IllegalStateException("body is null")

                Handler(Looper.getMainLooper()).post {
                    // 実行結果を出力
                    Log.d("CreateReportResponse", createReportResponse.toString())
                }
            }catch (e: Exception){
                // エラー内容を出力
                Log.e("error", e.message.toString())
            }
        }
    }

    //ユーザーIDを取得する
    private fun getUserId(): String {
        // SharePreferencesからユーザIDを取得
        val pref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val userId = pref.getString("userId", "").toString()
        return userId
    }
}