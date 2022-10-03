package com.example.procon33_remotetravelers_app.models.apis

import java.sql.Timestamp

data class GetReportAllResponse(
    val ok: Boolean,
    val album: List<GetReports>,
    val error: String?,
)

data class GetReports(
    val created_at: String,
    val image: String,
    val comment: String,
    val excitement: Int,
    val lat: Double,
    val lon: Double,
)
