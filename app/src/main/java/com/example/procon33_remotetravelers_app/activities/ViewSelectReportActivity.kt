package com.example.procon33_remotetravelers_app.activities

import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.procon33_remotetravelers_app.R

class ViewSelectReportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_select_report)

        val intent = intent
        val excitement = intent.getIntExtra("excitement", 0)
        val comment = intent.getStringExtra("comment")
        val bitmap = intent.getParcelableExtra<Bitmap>("image")

        val reportComment = findViewById<TextView>(R.id.report_comment)
        reportComment.text = comment

        val displayNumber = findViewById<TextView>(R.id.display_number)
        displayNumber.text = getString(R.string.percentage, excitement)

        val displayImage = findViewById<ImageView>(R.id.report_image)
        displayImage.setImageBitmap(bitmap)

        val backButton = findViewById<Button>(R.id.back_to_map_button)
        backButton.setOnClickListener{
            finish()
        }
    }
}