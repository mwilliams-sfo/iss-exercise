package com.example.iss.api.iss

import com.example.iss.api.iss.model.response.ISSNowResponse
import retrofit2.http.GET

interface ISSApi {
    @GET("/iss-now.json")
    suspend fun issNow(): ISSNowResponse
}