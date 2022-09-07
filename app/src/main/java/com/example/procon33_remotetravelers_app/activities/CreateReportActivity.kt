package com.example.procon33_remotetravelers_app.activities

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import com.example.procon33_remotetravelers_app.R

class CreateReportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_report)

        // TravelerActivityから写真データ(Bitmap)を取得する
        val intent = intent
        val photo = intent.getParcelableExtra<Bitmap>("data")

        // 受け取った写真データを表示
        val imageView = findViewById<ImageView>(R.id.report_image)
        imageView.setImageBitmap(photo)

        val keepButton = findViewById<Button>(R.id.keep_button)
        val backButton = findViewById<Button>(R.id.back_button)

        keepButton.setOnClickListener {
            finish()
        }

        backButton.setOnClickListener {
            finish()
        }
    }
}