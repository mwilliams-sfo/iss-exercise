package com.example.chase.api.iss

import com.example.chase.api.iss.model.response.ISSNowResponse
import retrofit2.http.GET

interface ISSApi {
    @GET("/iss-now.json")
    suspend fun issNow(): ISSNowResponse
}