package com.example.procon33_remotetravelers_app.activities

import android.graphics.Color
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions

class DrawRouteActivity {
    companion object{
        // 線の太さを15pxに設定
        private const val INITIAL_STROKE_WIDTH_PX = 15

        private var lastLatLng = LatLng(0.0, 0.0)

        //旅行者が通ったルート表示
        fun drawRoute(mMap: GoogleMap, currentLatLng: LatLng){

            if(lastLatLng != LatLng(0.0, 0.0)) {
                mMap.addPolyline(
                    PolylineOptions()
                        .add(lastLatLng, currentLatLng)
                        .width(INITIAL_STROKE_WIDTH_PX.toFloat())
                        .color(Color.parseColor("#766BF3FF"))
                        .geodesic(true)
                )
            }

            lastLatLng = currentLatLng
        }
    }
}