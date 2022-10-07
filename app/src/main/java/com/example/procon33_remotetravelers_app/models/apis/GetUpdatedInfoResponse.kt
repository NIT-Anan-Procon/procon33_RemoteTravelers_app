package com.example.procon33_remotetravelers_app.models.apis

data class GetUpdatedInfoResponse(
    val ok: Boolean,
    val error: String?,
    val current_location: Location?,
    val destination: List<Location?>?,
    val comments: List<Comment?>?,
    val situation: String?,
    val reports: List<Report?>?,
)