package com.example.procon33_remotetravelers_app.activities

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import com.example.procon33_remotetravelers_app.BuildConfig
import com.example.procon33_remotetravelers_app.R
import com.example.procon33_remotetravelers_app.models.apis.CreateReportResponse
import com.example.procon33_remotetravelers_app.services.CreateReportService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import rx.Subscriber
//import rx.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread

class CreateReportActivity : AppCompatActivity() {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
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

        //画像として保存する
        val context: Context = applicationContext

        //data/data/パッケージ名/app_name(ここではimage)ディレクトリにアクセスすることができる
        val directory = ContextWrapper(context).getDir(
            "image",
            Context.MODE_PRIVATE
        )

        val file = File(directory, "image_name.jpg")

        if(photo != null) {
            FileOutputStream(file).use { stream ->
                photo.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            }
        }

        //保存したファイルを取得
        val image = File("data/data/com.example.procon33_remotetravelers_app/app_image", "image_name.jpg")
        Log.d("image", image.toString())

        //ユーザーIDを取得
        val userId = getUserId().toInt()

        val imageMulti :MultipartBody? = fixImage(image)

        val keepButton = findViewById<Button>(R.id.keep_button)
        val backButton = findViewById<Button>(R.id.back_button)
        val commentText = findViewById<EditText>(R.id.comment)

        keepButton.setOnClickListener {
            Log.d("user_id", userId.toString())
            val comment = commentText.text.toString()
            sendReport(userId, image, comment, lat, lon)
            finish()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    //DBにレポートの内容を保存する(ネストが深くなりそうだったので関数にする)
//    private fun saveData(userId: Int, imageMulti:  MultipartBody?, comment: String, lat: Double, lon: Double){
//        thread {
//            try {
//                val service: CreateReportService =
//                    retrofit.create(CreateReportService::class.java)
//                val createReportResponse = service.createReport(
//                    user_id = userId, image = imageMulti, comment = comment, excitement = 1, lat = lat, lon = lon
//                ).execute().body()
//                    ?: throw IllegalStateException("body is null")
//
//                Handler(Looper.getMainLooper()).post {
//                    // 実行結果を出力
//                    Log.d("CreateReportResponse", createReportResponse.toString())
//                }
//            }catch (e: Exception){
//                // エラー内容を出力
//                Log.e("error", e.message.toString())
//            }
//        }
//    }

    //ユーザーIDを取得する
    private fun getUserId(): String {
        // SharePreferencesからユーザIDを取得
        val pref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val userId = pref.getString("userId", "").toString()
        return userId
    }

    //DBに送るための画像データに変換
    private fun fixImage(file: File): MultipartBody?{
        val requestBody = RequestBody.create(MediaType.parse("data/data/com.example.procon33_remotetravelers_app/app_image"), file)

        //ランダムな値の生成
//        val boundary = UUID.randomUUID().toString()
        val imageMulti = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", "image_name.jpg", requestBody)
            .build()

        return imageMulti
    }

    private fun sendReport(userId: Int, image:  File, comment: String, lat: Double, lon: Double){
        val map: MutableMap<String, RequestBody> = HashMap()

        val userId = RequestBody.create(MediaType.parse("text/plain"), userId.toString())
        val comment = RequestBody.create(MediaType.parse("text/plain"), comment)
        val excitment = RequestBody.create(MediaType.parse("text/plain"), "1")
        val lat = RequestBody.create(MediaType.parse("text/plain"), lat.toString())
        val lon = RequestBody.create(MediaType.parse("text/plain"), lon.toString())
        val image = RequestBody.create(MediaType.parse("image/jpg"), image)

        map.put("user_id", userId)
        map.put("image", image)
        map.put("commet", comment)
        map.put("excitment", excitment)
        map.put("lat", lat)
        map.put("lon", lon)

        thread {
            createApiClient().createReport(params = map)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Subscriber<CreateReportResponse>() {
                    override fun onNext(r: CreateReportResponse?) {  // 成功
                        Log.d("MainActivity", r.toString())
                    }

                    override fun onError(e: Throwable?) {
                        Log.d("error", e.toString())  // 失敗
                    }

                    override fun onCompleted() {
                    }
                })
        }
    }

    private fun createApiClient(): CreateReportService{
        val okClient = OkHttpClient()

        val builder = Retrofit.Builder()
            .client(okClient)
            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl(BuildConfig.API_URL)
            .build()
         return  builder.create(CreateReportService::class.java)
    }
}