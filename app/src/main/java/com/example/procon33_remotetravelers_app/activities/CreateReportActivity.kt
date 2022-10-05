package com.example.procon33_remotetravelers_app.activities

import android.content.Context
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.*
import com.example.procon33_remotetravelers_app.BuildConfig
import com.example.procon33_remotetravelers_app.R
import com.example.procon33_remotetravelers_app.models.apis.CreateReportResponse
import com.example.procon33_remotetravelers_app.services.CreateReportService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import retrofit2.Retrofit
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class CreateReportActivity : AppCompatActivity() {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_report)

        // TravelerActivityから写真データ(Bitmap)を取得する
        val intent = intent
        val photo = intent.getParcelableExtra<Bitmap>("data")

        //位置情報のデータの受け取り
        val lat = intent.getDoubleExtra("lat", 0.0)
        val lon = intent.getDoubleExtra("lon", 0.0)

        // 受け取った写真データを表示
        val imageView = findViewById<ImageView>(R.id.report_image)
        imageView.setImageBitmap(photo)

        //ユーザーIDを取得
        val userId = getUserId().toInt()

        val keepButton = findViewById<Button>(R.id.keep_button)
        val backButton = findViewById<Button>(R.id.back_button)
        val commentText = findViewById<EditText>(R.id.comment)
        val excitementSeekBar = findViewById<SeekBar>(R.id.excitement_seekBar)
        val displayNumber = findViewById<TextView>(R.id.display_number)

        excitementSeekBar.progress = 0
        excitementSeekBar.max = 100

        excitementSeekBar.setOnSeekBarChangeListener(
            object: SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    //つまみがドラッグされると呼び出される
                    val str: String = getString(R.string.percentage, progress)
                    displayNumber.text = str
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    //つまみがタッチされたときに呼ばれる
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    //つまみがリリースされたときに呼び出される
                }
            }
        )

        //base64をエンコーディングしたものを取得
        val image = encodeImage(photo) ?: ""

        keepButton.setOnClickListener {
            Log.d("user_id", userId.toString())
            //コメント内容の取得
            var comment = commentText.text.toString()
            if(comment == ""){
                comment = "コメントは入力されていません"
            }
            //感動パラメータの取得
            val excitement = displayNumber.text.toString().removeSuffix("%").toInt()
            sendReport(userId, image, comment, lat, lon, excitement)
            TravelerActivity.stopUpdateFlag = false
            finish()
        }

        backButton.setOnClickListener {
            TravelerActivity.stopUpdateFlag = false
            finish()
        }
    }

    //ユーザーIDを取得する
    private fun getUserId(): String {
        // SharePreferencesからユーザIDを取得
        val pref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        return pref.getString("userId", "").toString()
    }

    private fun sendReport(userId: Int, image:  String, comment: String, lat: Double, lon: Double, excitement: Int){
        val map: MutableMap<String, RequestBody> = HashMap()

        val userIdFix = RequestBody.create(MediaType.parse("text/plain"), userId.toString())
        val commentFix = RequestBody.create(MediaType.parse("text/plain"), comment)
        val excitementFix = RequestBody.create(MediaType.parse("text/plain"), excitement.toString())
        val latFix = RequestBody.create(MediaType.parse("text/plain"), lat.toString())
        val lonFix = RequestBody.create(MediaType.parse("text/plain"), lon.toString())
        val imageFix = RequestBody.create(MediaType.parse("text/plain"), image)

        map["user_id"] = userIdFix
        map["image"] = imageFix
        map["comment"] = commentFix
        map["excitement"] = excitementFix
        map["lat"] = latFix
        map["lon"] = lonFix

        thread {
            createApiClient().createReport(params = map)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<CreateReportResponse> {
                    override fun onNext(r: CreateReportResponse) {  // 成功
                        Handler(Looper.getMainLooper()).post {
                            Log.d("CreateReport", r.toString())
                        }
                        TravelerActivity.updateRequestFlag = true   //画面更新
                    }

                    override fun onError(e: Throwable) {    // 失敗
                        Handler(Looper.getMainLooper()).post {
                            Log.d("CreateReportError", e.toString())
                        }
                    }

                    override fun onSubscribe(d: Disposable) {
                    }

                    override fun onComplete() {
                    }
                })
        }
    }

    //okhttpとretrofitを使ってAPIを叩く
    private fun createApiClient(): CreateReportService{
        val okClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val builder = Retrofit.Builder()
            .client(okClient)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl(BuildConfig.API_URL)
        .build()
         return  builder.create(CreateReportService::class.java)
    }

    //Bitmapをbase64に変換
    private fun encodeImage(photo: Bitmap?): String?{
        if(photo != null){
            val byteArrayOutputStream = ByteArrayOutputStream()
            photo.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val b = byteArrayOutputStream.toByteArray()
            return Base64.encodeToString(b, Base64.DEFAULT)
        }
        return ""
    }
}