package com.example.procon33_remotetravelers_app.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.procon33_remotetravelers_app.R

class ViewReportActivity: AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_report)
        val index = intent.getIntExtra("index", 0)
        val marker = DisplayReportActivity.markers[index]
        val bitmap = DisplayReportActivity.bitmaps[index]

        val snippets = marker.snippet!!.split("/")
        val comment = snippets[0]
        val excitement = snippets[1].toInt()

        val reportComment = findViewById<TextView>(R.id.report_comment)
        reportComment.text = comment

        val displayNumber = findViewById<TextView>(R.id.display_number)
        displayNumber.text = excitement.toString() + "%"

        val displayImage = findViewById<ImageView>(R.id.report_image)
        displayImage.setImageBitmap(bitmap)

        val backButton = findViewById<Button>(R.id.back_to_map_button)
        backButton.setOnClickListener{
            finish()
        }
    }
}