package com.example.procon33_remotetravelers_app.models.apis

data class GetRootResponse(
    val routes: List<Route>?,
)

data class Route(
    val legs: List<Leg>,
)

data class Leg(
    val steps: List<Step>,
)

data class Step(
    val end_location: Point,
)

data class Point(
    val lat: Double,
    val lng: Double,
)
