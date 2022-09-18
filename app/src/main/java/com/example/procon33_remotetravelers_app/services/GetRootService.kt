package com.example.procon33_remotetravelers_app.services

import com.example.procon33_remotetravelers_app.models.apis.GetRootResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

sealed interface GetRootService {
    @GET("/maps/api/directions/json?")
    fun getRoot(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") key: String,
    ): Call<GetRootResponse>
}