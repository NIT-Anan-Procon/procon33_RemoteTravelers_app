package com.example.procon33_remotetravelers_app.services

import com.example.procon33_remotetravelers_app.models.apis.SuggestDestinationResponse
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface SuggestDestinationService {
    @FormUrlEncoded
    @POST("/api/common/save-location")
    fun suggestDestination(
        @Field("user_id") user_id: Int,
        @Field("lat") lat: Double,
        @Field("lon") lon: Double,
        @Field("suggestion_flag") suggestion_flag: Int,
    ): Call<SuggestDestinationResponse>
}