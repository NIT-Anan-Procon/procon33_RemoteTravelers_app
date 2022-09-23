package com.example.procon33_remotetravelers_app.activities

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.procon33_remotetravelers_app.models.apis.Location
import com.example.procon33_remotetravelers_app.models.apis.Route
import com.example.procon33_remotetravelers_app.services.GetRootService
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlin.concurrent.thread

class DisplayPinActivity {
    companion object {
        private val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        private val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        private var lastPins = mutableListOf<Marker>()
        private var polyline: Polyline? = null
        private var polylines: ArrayList<Polyline>? = arrayListOf()
        private var routesData: List<Route>? = arrayListOf()

        fun displayPin(mMap: GoogleMap, destinations: List<Location?>) {
            removePin()
            for (destination in destinations) {
                if(destination == null){
                    continue
                }
                val pin = mMap.addMarker(
                    MarkerOptions().position(LatLng(destination.lat, destination.lon))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )
                lastPins.add(pin!!)
            }
        }

        private fun removePin() {
            for (pin in lastPins) {
                pin.remove()
            }
            lastPins.clear()
        }

        fun displayRoot(mMap: GoogleMap ,currentLocation: LatLng, suggestLocation: LatLng) {
            try {
                getRoot(currentLocation, suggestLocation)
                while (true) {
                    Log.d("displayRoot", routesData.toString())
                    if (routesData != null) {
                        val routes = routesData!!
                        val route = routes[0]
                        val leg = route.legs[0]
                        val path: MutableList<LatLng> = ArrayList()
                        for (location in leg.steps) {
                            val point = LatLng(location.end_location.lat, location.end_location.lng)
                            path.add(point)
                        }
                        polylines = arrayListOf()
                        for (i in 1 until path.size) {
                            polyline = mMap.addPolyline(
                                PolylineOptions()
                                    .add(path[i - 1], path[i])
                                    .color(Color.CYAN)
                                    .width(20F)
                            )
                            polylines!!.add(polyline!!)
                        }
                        break
                    }
                }
            }catch (e: Exception){
                Log.e("displayRootError", e.message.toString())
            }
        }

        private fun getRoot(current: LatLng, suggest: LatLng) {
            thread {
                try {
                    // マーカの表示処理
                    val currentLocation = current.latitude.toString() + "," + current.longitude.toString()
                    val suggestLocation = suggest.latitude.toString() + "," + suggest.longitude.toString()
                    // APIを実行
                    val service: GetRootService =
                        retrofit.create(GetRootService::class.java)
                    val getRootResponse = service.getRoot(
                        origin = currentLocation, destination = suggestLocation, key = "AIzaSyAoQ-8XLGmUPOpGTrzVUnh0030oMkzORGU"
                    ).execute().body()
                        ?: throw IllegalStateException("body is null")

                    Handler(Looper.getMainLooper()).post {
                        // 実行結果を出力
                        Log.d("GetRoot", getRootResponse.toString())
                    }
                    routesData = getRootResponse.routes?.toList()
                    Handler(Looper.getMainLooper()).post {
                        // 実行結果を出力
                        Log.d("GetRoot", routesData.toString())
                    }
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post {
                        // エラー内容を出力
                        Log.e("getRootError", e.message.toString())
                    }
                }
            }
        }

        fun clearRoot(){
            //アプリは今までのポリラインを消去する。
            if (polylines != null) {
                polylines!!.forEach {
                    it.remove()
                }
            }
            Log.d("Root", "clearRoot")
        }
    }
}