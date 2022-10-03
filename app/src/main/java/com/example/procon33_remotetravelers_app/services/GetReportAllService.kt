package com.example.procon33_remotetravelers_app.services

import com.example.procon33_remotetravelers_app.models.apis.GetReportAllResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GetReportAllService {
    @GET("/api/common/get-album")
    fun getReportAll(
        @Query("user_id") user_id: Int,
    ): Call<GetReportAllResponse>
}