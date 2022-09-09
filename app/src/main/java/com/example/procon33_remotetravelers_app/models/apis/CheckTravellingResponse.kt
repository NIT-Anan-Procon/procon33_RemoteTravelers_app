package com.example.procon33_remotetravelers_app.models.apis

data class CheckTravellingResponse(
    val ok: Boolean,
    val traveling: Boolean?,
    val traveler: Boolean?,
    val error: String?
)
