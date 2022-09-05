package com.example.procon33_remotetravelers_app.services

import android.graphics.Bitmap
import com.example.procon33_remotetravelers_app.models.apis.CreateReportResponse
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface CreateReportService {
    @FormUrlEncoded
    @POST("/api/traveler/add-report")
    fun createReport(
        @Field("user_id")user_id: Int,
        @Field("image")image: Bitmap?,
        @Field("comment")comment: String,
        @Field("excitement")excitement: Int,
        @Field("lat")lat: Double,
        @Field("lon")lon: Double
    ): Call<CreateReportResponse>
}