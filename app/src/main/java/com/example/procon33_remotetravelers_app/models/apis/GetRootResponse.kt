package com.example.procon33_remotetravelers_app.models.apis

data class GetRootResponse(
    val routes: Route,
    val error_message: String,
)

data class Route(
    val Legs: List<Leg>,
)

data class Leg(
    val end_location: Point
)

data class Point(
    val lat: Double,
    val lng: Double,
)
