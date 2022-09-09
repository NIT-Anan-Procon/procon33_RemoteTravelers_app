package com.example.procon33_remotetravelers_app.services

import com.example.procon33_remotetravelers_app.models.apis.CheckTravelingResponse
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

sealed interface CheckTravelingService {
    @FormUrlEncoded
    @POST("/api/viewer/check-travel")
    fun checkTraveling(
        @Field("user_id")user_id: Int,
    ): Call<CheckTravelingResponse>
}