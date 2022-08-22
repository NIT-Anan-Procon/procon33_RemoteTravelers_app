package com.example.procon33_remotetravelers_app.services

import com.example.procon33_remotetravelers_app.models.apis.signupData.SignupResponse
import retrofit2.Call
import retrofit2.http.POST

interface SignupService {
    @POST("/api/user/signup")
    fun getUserId(): Call<SignupResponse>
}