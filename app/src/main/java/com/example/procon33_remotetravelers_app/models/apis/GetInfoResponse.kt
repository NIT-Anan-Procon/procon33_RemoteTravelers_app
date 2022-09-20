package com.example.procon33_remotetravelers_app.models.apis

data class GetInfoResponse(
    val ok: Boolean,
    val error: String?,
    val current_location: Location,
    val destination: List<Location?>,
    val route: List<Location?>,
    val comments: List<Comment?>,
    val situation: String?,
    val reports: List<Report?>,
)

data class Location(
    val lat: Double,
    val lon: Double,
)

data class Comment(
    val traveler: Int,
    val comment: String,
)

data class Report(
    val comment: String,
    val excitement: Int,
    val lat: Double,
    val lon: Double,
)