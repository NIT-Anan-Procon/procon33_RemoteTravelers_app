package com.example.procon33_remotetravelers_app.activities

import android.graphics.Color
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions

class DrawRoute {
    companion object{
        // 線の太さを15pxに設定
        private val INITIAL_STROKE_WIDTH_PX = 15

        var beforeLatLng = LatLng(0.0, 0.0)
        var i = 0.0

        fun drawRoute(mMap: GoogleMap, currentLatLng: LatLng){

            if(beforeLatLng != LatLng(0.0, 0.0)) {
                mMap.addPolyline(
                    PolylineOptions()
                        .add(beforeLatLng, currentLatLng)
                        .width(INITIAL_STROKE_WIDTH_PX.toFloat())
                        .color(Color.parseColor("#766BF3FF")).geodesic(true)
                )
            }

            beforeLatLng = currentLatLng
        }
    }
}