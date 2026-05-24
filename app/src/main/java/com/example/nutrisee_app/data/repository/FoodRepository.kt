package com.example.nutrisee.data.repository

import com.example.nutrisee.data.model.CalorieNinjasItem
import com.example.nutrisee.data.model.FoodSearchItem
import com.example.nutrisee.data.model.ParsedNutrition
import com.example.nutrisee.data.network.RetrofitClient

class FoodRepository {

    private val api    = RetrofitClient.calorieNinjasApi
    private val apiKey = "k5gm8K104/Nypo2R596wZQ==5uMkRZ07wLRYvHBd"

    suspend fun searchFood(query: String): Result<List<FoodSearchItem>> {
        return try {
            val response = api.getNutrition(apiKey = apiKey, query = query)
            val items = response.items.mapIndexed { index, item ->
                FoodSearchItem(
                    id        = index,
                    title     = item.name,
                    image     = null,
                    imageType = null
                )
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getNutrition(foodName: String): Result<ParsedNutrition> {
        return try {
            val response = api.getNutrition(apiKey = apiKey, query = foodName)
            val item     = response.items.firstOrNull()
                ?: return Result.failure(Exception("Data nutrisi tidak ditemukan untuk: $foodName"))

            val parsed = item.toParsedNutrition()
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun CalorieNinjasItem.toParsedNutrition() = ParsedNutrition(
        foodId   = 0,
        foodName = this.name,
        calories = this.calories,
        carbs    = this.carbohydratesTotalG,
        protein  = this.proteinG,
        fat      = this.fatTotalG,
        fiber    = this.fiberG
    )
}