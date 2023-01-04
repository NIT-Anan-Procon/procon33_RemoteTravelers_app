package com.example.procon33_remotetravelers_app.activities

import android.os.Handler
import android.os.Looper
import com.example.procon33_remotetravelers_app.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlin.concurrent.thread

class CurrentLocationActivity {
    private var track = false
    private var firstTrack = true
    private var setUpped: Boolean = false
    private var lastLocation = LatLng(0.0, 0.0)
    var currentLocationMarker: Marker? = null
    lateinit var currentLocation: LatLng

    //マップの拡大値・初期位置を定義
    fun initializeMap(mMap: GoogleMap){
        val tokyo = LatLng(35.90684931, 139.68896404)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(tokyo.latitude, 180 - tokyo.longitude)))
        thread {
            Thread.sleep(300)
            Handler(Looper.getMainLooper()).post {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(tokyo, 4f))
            }
        }
    }

    //現在地追跡ボタンが押されたときの処理
    fun pressedButton() : Pair<Int, Int>{
        track = !track
        firstTrack = track
        val text: Int
        val color: Int
        when(track){
            true -> {
                text = R.string.track_on
                color = R.drawable.track_on
            }
            else -> {
                text = R.string.track_off
                color = R.drawable.track_off
            }
        }
        return Pair(text, color)
    }

    //現在地を表示
    fun displayCurrentLocation(mMap: GoogleMap, location: LatLng){
        currentLocation = location
        if (lastLocation != currentLocation) {
            currentLocationMarker?.remove()
            currentLocationMarker =
                mMap.addMarker(MarkerOptions().position(currentLocation).title("現在地").zIndex(10f))
            if (track) {
                mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLocation))
            }
            lastLocation = currentLocation
        }
        if (firstTrack) {
            currentLocationMarker?.remove()
            currentLocationMarker =
                mMap.addMarker(MarkerOptions().position(currentLocation).title("現在地").zIndex(10f))
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
            if (!setUpped) {
                thread {
                    Thread.sleep(2000)
                    Handler(Looper.getMainLooper()).post {
                        mMap.setMinZoomPreference(7f)
                    }
                }
                setUpped = false
            }
            firstTrack = false
        }
    }
}