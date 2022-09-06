package com.example.procon33_remotetravelers_app.services

import android.graphics.Bitmap
import com.example.procon33_remotetravelers_app.models.apis.CreateReportResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface CreateReportService {
    @POST("/api/traveler/add-report")
    fun createReport(
        @Header("user_id")user_id: Int,
        @Body image: MultipartBody?,
        @Header("comment")comment: String,
        @Header("excitement")excitement: Int,
        @Header("lat")lat: Double,
        @Header("lon")lon: Double
    ): Call<CreateReportResponse>
}