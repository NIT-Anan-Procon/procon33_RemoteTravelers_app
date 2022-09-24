package com.example.procon33_remotetravelers_app.activities

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.procon33_remotetravelers_app.models.apis.Location
import com.example.procon33_remotetravelers_app.models.apis.Route
import com.example.procon33_remotetravelers_app.services.GetRouteService
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
        private var lastCurrentLocation: LatLng? = null
        private var lastSuggestLocation: LatLng? = null

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

        fun displayRoute(mMap: GoogleMap ,currentLocation: LatLng, suggestLocation: LatLng) {
            clearRoute()
            thread {
                try {
                    if(lastCurrentLocation != currentLocation || lastSuggestLocation != suggestLocation) {
                        getRoute(currentLocation, suggestLocation)
                        Thread.sleep(500)
                    }
                    if (routesData.isNullOrEmpty()) {
                        throw Exception("routes data not found")
                    }
                    val routes = routesData!!
                    val route = routes[0]
                    val leg = route.legs[0]
                    val path = mutableListOf<LatLng>()
                    path.add(currentLocation)
                    for (location in leg.steps) {
                        val point = LatLng(location.end_location.lat, location.end_location.lng)
                        path.add(point)
                    }
                    polylines = arrayListOf()
                    Handler(Looper.getMainLooper()).post {
                        Log.d("displayRoute", routesData.toString())
                        for (i in 1 until path.size) {
                            polyline = mMap.addPolyline(
                                PolylineOptions()
                                    .add(path[i - 1], path[i])
                                    .color(Color.GREEN)
                                    .width(20F)
                            )
                            polylines!!.add(polyline!!)
                        }
                    }
                    lastCurrentLocation = currentLocation
                    lastSuggestLocation = suggestLocation
                }catch (e: Exception){
                    Handler(Looper.getMainLooper()).post {
                        Log.e("displayRouteError", e.message.toString())
                    }
                }
            }

        }

        private fun getRoute(current: LatLng, suggest: LatLng) {
            thread {
                try {
                    // マーカの表示処理
                    val currentLocation = current.latitude.toString() + "," + current.longitude.toString()
                    val suggestLocation = suggest.latitude.toString() + "," + suggest.longitude.toString()
                    // APIを実行
                    val service: GetRouteService =
                        retrofit.create(GetRouteService::class.java)
                    val getRouteResponse = service.getRoute(
                        origin = currentLocation, destination = suggestLocation, key = "AIzaSyAoQ-8XLGmUPOpGTrzVUnh0030oMkzORGU"
                    ).execute().body()
                        ?: throw IllegalStateException("body is null")
                    routesData = getRouteResponse.routes?.toList()
                    Handler(Looper.getMainLooper()).post {
                        // 実行結果を出力
                        Log.d("GetRoute", routesData.toString())
                    }
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post {
                        // エラー内容を出力
                        Log.e("getRouteError", e.message.toString())
                    }
                }
            }
        }

       fun clearRoute(){
            //アプリは今までのポリラインを消去する。
           if(polylines != null) {
               polylines!!.forEach {
                   it.remove()
               }
           }
            Log.d("Route", "clearRoute")
        }
    }
}