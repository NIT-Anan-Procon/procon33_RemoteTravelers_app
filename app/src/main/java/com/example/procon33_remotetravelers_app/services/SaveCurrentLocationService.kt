package com.example.procon33_remotetravelers_app.services

import com.example.procon33_remotetravelers_app.models.apis.SaveCurrentLocationResponce
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

sealed interface SaveCurrentLocationService {
    @FormUrlEncoded
    @POST("/api/common/save-location")
    fun saveCurrentLocation(
        @Field("user_id") user_id: Int,
        @Field("lat") lat: Double,
        @Field("lon") lon: Double,
        @Field("suggestion_flag") suggestion_flag: Int,
    ): Call<SaveCurrentLocationResponce>
}