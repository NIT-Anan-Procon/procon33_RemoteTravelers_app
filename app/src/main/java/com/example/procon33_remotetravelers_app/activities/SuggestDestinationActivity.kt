package com.example.procon33_remotetravelers_app.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.example.procon33_remotetravelers_app.R

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.procon33_remotetravelers_app.databinding.ActivitySuggestDestinationBinding
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker

class SuggestDestinationActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnMapClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivitySuggestDestinationBinding
    private lateinit var suggestDestination: LatLng
    private var suggestMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySuggestDestinationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val button = findViewById<Button>(R.id.determine_pin_button)
        button.setOnClickListener {
            //ここで最終的なピンの情報をDBに保存(APIを叩く)

            val intent = Intent(this, ViewerActivity::class.java)
            startActivity(intent)
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

        mMap.setOnMapClickListener(this)
    }

    override fun onMapClick(point: LatLng) {
        suggestDestination = point
        suggestMarker?.remove()
        suggestMarker = mMap.addMarker(MarkerOptions().position(point).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))

    }
}