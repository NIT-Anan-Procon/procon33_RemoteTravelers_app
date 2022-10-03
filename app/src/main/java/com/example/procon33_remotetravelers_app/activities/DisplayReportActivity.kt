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
        private const val MARKER_WIDTH = 200
        var bitmaps = mutableListOf<Bitmap>()
        var markers = mutableListOf<Marker>()

        @RequiresApi(Build.VERSION_CODES.O)
        fun createReportMarker(mMap: GoogleMap, reports: List<Report?>, visible: Boolean){
            for(report in reports){
                report!!
                val image = Base64.getDecoder().decode(report.image)
                val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)
                bitmaps.add(bitmap)
                val aspectRatio = bitmap.width.toDouble() / bitmap.height
                markers.add(
                    mMap.addMarker(
                        MarkerOptions()
                        .position(LatLng(report.lat, report.lon))
                        .infoWindowAnchor(0.5f, 1.0f)
                        .icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmap, MARKER_WIDTH, (MARKER_WIDTH / aspectRatio).toInt(), true)))
                        .snippet(report.comment + "/" + report.excitement)
                        .alpha(
                            when(visible){
                                true -> 1f
                                else -> 0f
                            }
                        )
                    )!!
                )
            }
        }
    }
}

class CustomInfoWindow(private val context: Context) : GoogleMap.InfoWindowAdapter, GoogleMap.OnInfoWindowCloseListener {
    companion object {
        private const val INFO_WINDOW_WIDTH = 500
    }
    override fun getInfoContents(marker: Marker): View? = null

    override fun getInfoWindow(marker: Marker): View? = setupWindow(marker)

    override fun onInfoWindowClose(marker: Marker) {
        //透明にしたマーカーを元に戻す
        marker.alpha = 1f
    }

    private fun setupWindow(marker: Marker): View? {
        if (!markers.contains(marker)) {
            return null
        }
        val bitmap = bitmaps[markers.indexOf(marker)]
        val aspectRatio = bitmap.width.toDouble() / bitmap.height
        return LayoutInflater.from(context).inflate(R.layout.info_window, null, false).apply {
            findViewById<ImageView>(R.id.imageView).setImageBitmap(Bitmap.createScaledBitmap(bitmap, INFO_WINDOW_WIDTH, (INFO_WINDOW_WIDTH / aspectRatio).toInt(), true))
        }
    }
}