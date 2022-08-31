package com.example.procon33_remotetravelers_app.services

import com.example.procon33_remotetravelers_app.models.apis.startTravelData.StartTravelResponse
import retrofit2.Call
import retrofit2.http.*
import retrofit2.http.Field

interface StartTravelService {
    @FormUrlEncoded
    @POST("/api/traveler/start-travel")
    fun startTravel(
        @Field("host") host: Int,
        @Field("viewer1") viewer1: Int,
        @Field("viewer2") viewer2: Int,
        @Field("viewer3") viewer3: Int,
    ):Call<StartTravelResponse>
}