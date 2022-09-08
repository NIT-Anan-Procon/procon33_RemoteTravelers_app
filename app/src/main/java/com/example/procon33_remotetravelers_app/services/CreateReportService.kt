package com.example.procon33_remotetravelers_app.services

import com.example.procon33_remotetravelers_app.models.apis.CreateReportResponse
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*
import rx.Observable
import java.util.*

interface CreateReportService{
    @JvmSuppressWildcards
    @Multipart
    @POST("/api/traveler/add-report")
    fun createReport(
//        @Part("user_id")user_id: Int,
//        @Part("image")image: MultipartBody?,
//        @Part("comment")comment: String,
//        @Part("excitement")excitement: Int,
//        @Part("lat")lat: Double,
//        @Part("lon")lon: Double
        @PartMap params: Map<String, RequestBody>
    ): Observable<CreateReportResponse>
}