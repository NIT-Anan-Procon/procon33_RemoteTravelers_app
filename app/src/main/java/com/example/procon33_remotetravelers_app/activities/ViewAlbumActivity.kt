package com.example.procon33_remotetravelers_app.activities

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.procon33_remotetravelers_app.BuildConfig
import com.example.procon33_remotetravelers_app.R
import com.example.procon33_remotetravelers_app.services.GetReportAllService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.nio.ByteBuffer
import java.sql.Timestamp
import java.util.Base64.getDecoder
import kotlin.concurrent.thread

class ViewAlbumActivity : AppCompatActivity() {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_album)
        val userId = intent.getIntExtra("userId", 0)

        getReportAll(userId)
    }

    private fun getReportAll(userId: Int){
        thread{
            try {
                // APIを実行
                val service: GetReportAllService =
                    retrofit.create(GetReportAllService::class.java)
                val getReportAllResponse = service.getReportAll(
                    user_id = userId
                ).execute().body()
                    ?: throw IllegalStateException("body is null")

                Handler(Looper.getMainLooper()).post {
                    // 実行結果を出力
                    Log.d("GetReportAllResponse", getReportAllResponse.toString())
                }

                for(i in getReportAllResponse.album){
                    setReport(i.image, i.comment, i.created_at)
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    // エラー内容を出力
                    Log.e("GetReportAllError", e.message.toString())
                }
            }
        }
    }

    private fun setReport(image: String, comment: String, created_at: String){
        try {
            val WC = LinearLayout.LayoutParams.WRAP_CONTENT
            val MP = LinearLayout.LayoutParams.MATCH_PARENT

            val albumFrame = findViewById<LinearLayout>(R.id.album_list)
            val reportFrame = findViewById<LinearLayout>(R.id.report_liner)

            albumFrame.removeAllViews()
            reportFrame.removeAllViews()

            val decode = Base64.decode(image, Base64.DEFAULT)
            val bitmap = Bitmap.createBitmap(16, 9, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(decode))

            val image = ImageView(this)
            image.setImageBitmap(bitmap)
            reportFrame.addView(image, LinearLayout.LayoutParams(MP, WC))

            val text = TextView(this)
            text.setText(created_at + comment)
            reportFrame.addView(text, LinearLayout.LayoutParams(MP, WC))

            albumFrame.addView(image, LinearLayout.LayoutParams(MP, WC))
            albumFrame.addView(text, LinearLayout.LayoutParams(MP, WC))
        }catch (e: Exception){
            Handler(Looper.getMainLooper()).post {
                // エラー内容を出力
                Log.e("getCommentError", e.message.toString())
            }
        }
    }
}