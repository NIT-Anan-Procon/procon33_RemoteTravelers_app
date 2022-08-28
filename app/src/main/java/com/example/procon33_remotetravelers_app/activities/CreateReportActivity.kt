package com.example.procon33_remotetravelers_app.activities

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import com.example.procon33_remotetravelers_app.R

class CreateReportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_report)

        val intent = intent
        val photo = intent.getParcelableExtra<Bitmap>("data")

        val imageView = findViewById<ImageView>(R.id.report_image)
        imageView.setImageBitmap(photo)
    }
}