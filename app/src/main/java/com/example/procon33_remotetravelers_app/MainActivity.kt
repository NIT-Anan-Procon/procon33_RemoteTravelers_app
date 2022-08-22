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
import com.example.procon33_remotetravelers_app.services.SignupService
import com.example.procon33_remotetravelers_app.services.StartTravelService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
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

        // ユーザIDが登録されていれば表示し、無ければ新規登録APIをたたき表示する
        var userId = getUserId()
        val userIdText = findViewById<TextView>(R.id.userIdText)
        if (userId!!.isNotEmpty()) {
            userId = getString(R.string.user_id_text, userId)
            userIdText.text = userId
        } else {
            signup(userIdText)
        }

        // 旅行するボタンが押されるとTravelerActivityに遷移する
        val travelButton = findViewById<Button>(R.id.travel_button)
        travelButton.setOnClickListener {
            startTravel()
            val intent = Intent(this, TravelerActivity::class.java)
            startActivity(intent)
        }
    }

    private fun signup(userIdText: TextView) {
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

    private fun startTravel() {
        thread {
            try {
                val userId = getUserId().toInt()
                val viewer1 = findViewById<EditText>(R.id.viewer1)
                val viewer2 = findViewById<EditText>(R.id.viewer2)
                val viewer3 = findViewById<EditText>(R.id.viewer3)
                val viewersTextView = arrayOf(viewer1, viewer2, viewer3)
                val viewers = mutableListOf<Int>()
                for (viewerTextView in viewersTextView) {
                    if (viewerTextView.text.isNotEmpty()) {
                        viewers.add(viewerTextView.text.toString().toInt())
                    }
                }

                startTravelRequest(userId, viewers)

                val service: StartTravelService =
                    retrofit.create(StartTravelService::class.java)
                val startTravelResponse = service.startTravel(
                    userId,
                    viewers
                ).execute().body()

                Handler(Looper.getMainLooper()).post {
                    Log.d("viewers", viewers.toString())
                    Log.d("startTravelResponse", startTravelResponse.toString())
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    Log.e("error", e.message.toString())
                    val toast =
                        Toast.makeText(this, "旅行を始めることができませんでした", Toast.LENGTH_SHORT)
                    toast.show()
                }
            }
        }
    }

    public class startTravelRequest {
        var host: Int? = null
        var viewers: MutableList<Int>? = null

        fun startTravelRequest(host: Int, viewers: MutableList<Int>) {
            this.host = host
            this.viewers = viewers
        }
    }

    private fun getUserId(): String {
        // SharePreferencesからユーザIDを取得
        val pref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val userId = pref.getString("userId", "").toString()
        return userId
    }
}
