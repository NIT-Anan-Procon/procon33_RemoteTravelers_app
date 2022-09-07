package com.example.procon33_remotetravelers_app.activities

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.procon33_remotetravelers_app.R

import com.example.procon33_remotetravelers_app.databinding.ActivityViewerBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class ViewerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = intent.getIntExtra("userId", 0)

        binding = ActivityViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val button = findViewById<Button>(R.id.pin_button)
        button.setOnClickListener {
            val intent = Intent(this, SuggestDestinationActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        var fragment = false
        val button_comment = findViewById<Button>(R.id.comment_door_button)
        button_comment.setOnClickListener {
            fragment = !fragment
            moveComment(fragment)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    private fun moveComment(fragment: Boolean) {
        val target: View = findViewById(R.id.comments) // 対象となるオブジェクト
        val destination = if (fragment) -550f else 0f
        ObjectAnimator.ofFloat(target, "translationY", destination).apply {
            duration = 200 // ミリ秒
            start() // アニメーション開始
        }
    }
}
