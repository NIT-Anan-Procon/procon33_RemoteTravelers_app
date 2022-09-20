package com.example.procon33_remotetravelers_app.activities

import com.example.procon33_remotetravelers_app.models.apis.Location
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class DisplayPinActivity {
    companion object {
        private var lastPins = mutableListOf<Marker>()

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
    }
}