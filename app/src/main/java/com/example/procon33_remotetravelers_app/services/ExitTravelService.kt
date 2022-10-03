package com.example.procon33_remotetravelers_app.services

import com.example.procon33_remotetravelers_app.models.apis.ExitTravelResponse
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ExitTravelService {
    @FormUrlEncoded
    @POST("/api/traveler/finish-travel")
    fun exitTravel(
        @Field("user_id") user_id: Int
    ): Call<ExitTravelResponse>
}