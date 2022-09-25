package com.example.procon33_remotetravelers_app.services

import com.example.procon33_remotetravelers_app.models.apis.GetRouteResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

sealed interface GetRouteService {
    @GET("/maps/api/directions/json?")
    fun getRoute(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") key: String,
    ): Call<GetRouteResponse>
}