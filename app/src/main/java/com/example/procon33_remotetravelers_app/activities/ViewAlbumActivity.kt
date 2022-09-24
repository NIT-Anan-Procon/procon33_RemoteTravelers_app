package com.example.procon33_remotetravelers_app.activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.*
import com.example.procon33_remotetravelers_app.BuildConfig
import com.example.procon33_remotetravelers_app.R
import com.example.procon33_remotetravelers_app.models.apis.GetReports
import com.example.procon33_remotetravelers_app.services.GetReportAllService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
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

                setReport(getReportAllResponse.album)

            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    // エラー内容を出力
                    Log.e("GetReportAllError", e.message.toString())
                }
            }
        }
    }

    private fun setReport(reports: List<GetReports>){
        //別スレッドでUIをいじるための回避策
        runOnUiThread {
            try {
                val WC = LinearLayout.LayoutParams.WRAP_CONTENT
                val MP = LinearLayout.LayoutParams.MATCH_PARENT

                val albumLiner = findViewById<LinearLayout>(R.id.album_list)
                val reportLiner = findViewById<LinearLayout>(R.id.report_liner)
                val albumFrame = findViewById<FrameLayout>(R.id.report_album_frame)

                albumLiner.removeAllViews()
                reportLiner.removeAllViews()
                albumFrame.removeAllViews()

                for (report in reports){
                    val decodedBytes = Base64.decode(
                        report.image.substring(report.image.indexOf(",") + 1),
                        Base64.DEFAULT
                    )
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                    reportLiner.removeAllViews()

                    reportLiner.addView(
                        setImageView(bitmap),
                        LinearLayout.LayoutParams(WC, MP)
                    )

                    val date = report.created_at.replace("-", "/").replace("T", " ").replace(".000000Z", "")

                    reportLiner.addView(
                        setTextView(date + "\n" + report.comment),
                        LinearLayout.LayoutParams(WC, MP)
                    )

                    albumFrame.addView(
                        reportLiner,
                        LinearLayout.LayoutParams(MP, WC)
                    )

                    albumFrame.addView(
                          setButton(),
                        LinearLayout.LayoutParams(MP, MP)
                    )

                    albumLiner.addView(
                        albumFrame,
                        LinearLayout.LayoutParams(MP, WC)
                    )
                }

            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    // エラー内容を出力
                    Log.e("setReport", e.message.toString())
                }
            }
        }
    }

    private fun setTextView (commentText: String): TextView{
        val comment = TextView(this)
        comment.text = commentText
        comment.textSize = 20f
        comment.setPadding(10, 15, 10, 15)
        return comment
    }

    private fun setImageView(bitmap: Bitmap): ImageView{
        val image = ImageView(this)
        image.setImageBitmap(bitmap)
        return image
    }

    private fun setButton(): Button{
        val button = Button(this)
        //遷移できるように透明のボタンにする
        button.background = getDrawable(R.color.fide_report_button)
        return button
    }
}