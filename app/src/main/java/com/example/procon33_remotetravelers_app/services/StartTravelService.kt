package com.example.procon33_remotetravelers_app.services

import com.example.procon33_remotetravelers_app.models.apis.startTravelData.StartTravelResponse
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface StartTravelService {
    @FormUrlEncoded
    @POST("/api/traveler/start-travel")
    fun startTravel(
        @Field("host") host: Int,
        @Field("viewers") viewers: List<Int>
    ): Call<StartTravelResponse>
}