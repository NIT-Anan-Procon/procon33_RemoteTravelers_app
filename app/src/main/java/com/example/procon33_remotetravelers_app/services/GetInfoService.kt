package com.example.procon33_remotetravelers_app.services

import com.example.procon33_remotetravelers_app.models.apis.GetInfoResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

sealed interface GetInfoService {
    @GET("/api/common/get-info")
    fun getInfo(
        @Query("user_id") user_id: Int,
    ): Call<GetInfoResponse>
}