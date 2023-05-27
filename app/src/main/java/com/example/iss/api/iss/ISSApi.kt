package com.example.iss.api.iss

import com.example.iss.api.iss.model.response.AstrosResponse
import com.example.iss.api.iss.model.response.ISSNowResponse
import retrofit2.http.GET

interface ISSApi {
    @GET("iss-now.json")
    suspend fun issNow(): ISSNowResponse

    @GET("astros.json")
    suspend fun astros(): AstrosResponse
}