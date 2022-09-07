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

        binding = ActivityViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(com.example.procon33_remotetravelers_app.R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val button = findViewById<Button>(com.example.procon33_remotetravelers_app.R.id.pin_button)
        button.setOnClickListener {
            val intent = Intent(this, SuggestDestinationActivity::class.java)
            startActivity(intent)
            finish()
        }

        var fragment = false
        val button_comment = findViewById<Button>(com.example.procon33_remotetravelers_app.R.id.comment_door_button)
        button_comment.setOnClickListener {
            fragment = !fragment
            openComment(fragment)
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    private fun openComment(fragment: Boolean) {
        val target: View = findViewById(R.id.comments) // 対象となるオブジェクト
        val destination = if (fragment) -550f else 0f
        ObjectAnimator.ofFloat(target, "translationY", destination).apply {
            duration = 200 // ミリ秒
            start() // アニメーション開始
        }
    }
}