package com.example.procon33_remotetravelers_app.activities

import android.content.Intent
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
    companion object{
        const val WC = LinearLayout.LayoutParams.WRAP_CONTENT
        const val MP = LinearLayout.LayoutParams.MATCH_PARENT
    }

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

        val albumBack = findViewById<Button>(R.id.album_back_button)
        albumBack.setOnClickListener {
            finish()
        }
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
                val albumLinear = findViewById<LinearLayout>(R.id.album_list)
                albumLinear.removeAllViews()
                albumLinear.layoutParams.height = 300

                for (i in (reports.count() - 1) downTo 0){
                    val report = reports[i]
                    val decodedBytes = Base64.decode(
                        report.image.substring(report.image.indexOf(",") + 1),
                        Base64.DEFAULT
                    )
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                    val reportLinear = setLinear()

                    reportLinear.addView(
                        setImageView(bitmap),
                        LinearLayout.LayoutParams(300, 300)
                    )

                    val date = report.created_at.replace("-", "/").replace("T", " ").replace(".000000Z", "")

                    reportLinear.addView(
                        setTextView(date + "\n" + report.comment),
                        LinearLayout.LayoutParams(WC, 300)
                    )

                    val albumFrame = setFrame()

                    albumFrame.addView(
                        reportLinear,
                        LinearLayout.LayoutParams(MP, WC)
                    )

                    albumFrame.addView(
                          setButton(report, bitmap),
                        LinearLayout.LayoutParams(MP, MP)
                    )

                    albumLinear.addView(
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
        comment.setPadding(20, 15, 20, 15)
        comment.maxLines = 2
        return comment
    }

    private fun setImageView(bitmap: Bitmap): ImageView{
        val image = ImageView(this)
        image.setImageBitmap(bitmap)
        return image
    }

    var count = 0

    private fun setButton(report: GetReports, bitmap: Bitmap): Button{
        count++
        val button = Button(this)
        //遷移できるように透明のボタンにする
        button.background = getDrawable(R.color.hide_report_button)
        //id指定(なぜかIntしか無理)
        button.id = count

        button.setOnClickListener {
            val intent = Intent(this, ViewSelectReportActivity::class.java)
            intent.putExtra("excitement", report.excitement)
            intent.putExtra("comment", report.comment)
            intent.putExtra("image", bitmap)
            startActivity(intent)
        }
        return button
    }

    private fun setLinear(): LinearLayout{
        val linear = LinearLayout(this)
        linear.orientation = LinearLayout.HORIZONTAL
        linear.weightSum = 3.0F
        return linear
    }

    private fun setFrame(): FrameLayout{
        val frame = FrameLayout(this)
        frame.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        frame.setPadding(10, 10, 10, 10)
        return frame
    }
}