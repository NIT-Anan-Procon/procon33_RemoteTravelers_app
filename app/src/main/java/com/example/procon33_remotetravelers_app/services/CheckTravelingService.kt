package com.example.procon33_remotetravelers_app.services

import com.example.procon33_remotetravelers_app.models.apis.CheckTravelingResponse
import retrofit2.Call
import retrofit2.http.*

sealed interface CheckTravelingService {
    @GET("/api/viewer/check-travel")
    fun checkTraveling(
        @Query("user_id")user_id: Int,
    ): Call<CheckTravelingResponse>
}