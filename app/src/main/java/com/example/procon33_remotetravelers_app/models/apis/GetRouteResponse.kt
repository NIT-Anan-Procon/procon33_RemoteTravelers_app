package com.example.procon33_remotetravelers_app.models.apis

data class GetRouteResponse(
    val routes: List<Route>?,
)

data class Route(
    val legs: List<Leg>,
)

data class Leg(
    val steps: List<Step>,
)

data class Step(
    val polyline: Points,
)

data class Points(
    val points: String,
)