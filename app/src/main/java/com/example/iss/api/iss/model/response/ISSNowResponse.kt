package com.example.iss.api.iss.model.response

import androidx.annotation.Keep
import com.example.iss.api.iss.model.Position
import com.google.gson.annotations.SerializedName

@Keep
data class ISSNowResponse(
    val message: String,
    val timestamp: Long,
    @SerializedName("iss_position")
    val issPosition: Position
)
