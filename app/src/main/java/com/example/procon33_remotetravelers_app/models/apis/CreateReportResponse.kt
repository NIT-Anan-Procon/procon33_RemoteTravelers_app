package com.example.procon33_remotetravelers_app.models.apis

import java.io.Serializable

data class CreateReportResponse(
    val error: String?,
    val ok: Boolean,
): Serializable
