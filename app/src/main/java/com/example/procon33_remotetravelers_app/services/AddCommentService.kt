package com.example.procon33_remotetravelers_app.services

import com.example.procon33_remotetravelers_app.models.apis.AddCommentResponse
import retrofit2.Call
import retrofit2.http.*
import retrofit2.http.Field

interface AddCommentService {
    @FormUrlEncoded
    @POST("/api/common/add-comment")
    fun addComment(
        @Field("user_id") user_id: Int,
        @Field("comment") comment: String,
    ): Call<AddCommentResponse>
}