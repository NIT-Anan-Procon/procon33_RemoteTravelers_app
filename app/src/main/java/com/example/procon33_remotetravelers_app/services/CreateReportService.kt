package com.example.procon33_remotetravelers_app.services

import com.example.procon33_remotetravelers_app.models.apis.CreateReportResponse
import io.reactivex.Observable
import okhttp3.RequestBody
import retrofit2.http.*

interface CreateReportService{
    @JvmSuppressWildcards
    @Multipart
    @POST("/api/traveler/add-report")
    fun createReport(
        @PartMap params: Map<String, RequestBody>
    ): Observable<CreateReportResponse>
}