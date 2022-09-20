package com.example.procon33_remotetravelers_app.activities

import android.view.View
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.annotation.RequiresApi
import com.example.procon33_remotetravelers_app.R
import com.example.procon33_remotetravelers_app.activities.DisplayReportActivity.Companion.bitmaps
import com.example.procon33_remotetravelers_app.activities.DisplayReportActivity.Companion.markers
import com.example.procon33_remotetravelers_app.models.apis.Report
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*

class DisplayReportActivity {
    companion object{
        var bitmaps = mutableListOf<Bitmap>()
        var markers = mutableListOf<Marker>()

        @RequiresApi(Build.VERSION_CODES.O)
        fun displayReport(mMap: GoogleMap, reports: List<Report?>){
            for(report in reports){
                report!!
                val image = Base64.getDecoder().decode(report.image)
                val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)
                bitmaps.add(Bitmap.createScaledBitmap(bitmap, 500, 500, true))
                markers.add(
                    mMap.addMarker(
                        MarkerOptions()
                        .position(LatLng(report.lat, report.lon))
                        .infoWindowAnchor(0.5f, 0.5f)
                        .icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, 150, 150, true)))
                    )!!
                )
            }
        }
    }
}

class CustomInfoWindow(private val context: Context) : GoogleMap.InfoWindowAdapter, GoogleMap.OnInfoWindowCloseListener {
    override fun getInfoContents(marker: Marker): View? = null

    override fun getInfoWindow(marker: Marker): View? = setupWindow(marker)

    override fun onInfoWindowClose(marker: Marker) {
        marker.alpha = 1f
    }

    private fun setupWindow(marker: Marker): View? {
        if (!markers.contains(marker)) {
            return null
        }
        val bitmap = bitmaps[markers.indexOf(marker)]
        return LayoutInflater.from(context).inflate(R.layout.info_window, null, false).apply {
            findViewById<ImageView>(R.id.imageView).setImageBitmap(bitmap)
        }
    }
}