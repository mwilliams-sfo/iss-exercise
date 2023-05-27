package com.example.iss.api.iss.model.response

import com.example.iss.api.iss.model.Position
import com.google.gson.annotations.SerializedName

data class ISSNowResponse(
    val message: String,
    @SerializedName("iss_position")
    val issPosition: Position,
    val timestamp: Long
)
