package com.example.procon33_remotetravelers_app.services

import com.example.procon33_remotetravelers_app.models.apis.CheckTravellingResponse
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.sql.Timestamp

sealed interface CheckTravellingService {
    @FormUrlEncoded
    @POST("/api/viewer/check-travel")
    fun checkTravelling(
        @Field("user_id")user_id: Int,
        @Field("last_request")last_request: Timestamp,
    ): Call<CheckTravellingResponse>
}