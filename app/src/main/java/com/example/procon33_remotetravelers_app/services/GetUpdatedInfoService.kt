package com.example.procon33_remotetravelers_app.services

import com.example.procon33_remotetravelers_app.models.apis.GetUpdatedInfoResponse
import retrofit2.Call
import retrofit2.http.*

interface GetUpdatedInfoService {
    @FormUrlEncoded
    @POST("/api/common/update-info")
    fun getUpdatedInfo(
        @Field("user_id") user_id: Int,
    ): Call<GetUpdatedInfoResponse>
}