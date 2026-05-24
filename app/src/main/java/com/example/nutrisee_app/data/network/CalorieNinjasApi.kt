package com.example.nutrisee.data.network

import com.example.nutrisee.data.model.CalorieNinjasResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface CalorieNinjasApi {

    @GET("v1/nutrition")
    suspend fun getNutrition(
        @Header("X-Api-Key") apiKey: String,
        @Query("query")      query:  String
    ): CalorieNinjasResponse

}