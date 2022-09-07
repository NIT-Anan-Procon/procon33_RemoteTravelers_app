package com.example.procon33_remotetravelers_app.services

import android.graphics.Bitmap
import com.example.procon33_remotetravelers_app.models.apis.CreateReportResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

sealed interface CreateReportService{
    @Multipart
    @POST("/api/traveler/add-report")
    fun createReport(
        @Part("user_id")user_id: Int,
        @Part("image")image: MultipartBody?,
        @Part("comment")comment: String,
        @Part("excitement")excitement: Int,
        @Part("lat")lat: Double,
        @Part("lon")lon: Double
    ): Call<CreateReportResponse>
}