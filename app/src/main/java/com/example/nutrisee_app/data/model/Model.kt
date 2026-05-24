package com.example.nutrisee.data.model

import com.google.gson.annotations.SerializedName
data class CalorieNinjasResponse(
    @SerializedName("items")
    val items: List<CalorieNinjasItem> = emptyList()
)

data class CalorieNinjasItem(
    @SerializedName("name")
    val name: String,
    @SerializedName("serving_size_g")
    val servingSizeG: Float,
    @SerializedName("calories")
    val calories: Float,
    @SerializedName("protein_g")
    val proteinG: Float,
    @SerializedName("fat_total_g")
    val fatTotalG: Float,
    @SerializedName("carbohydrates_total_g")
    val carbohydratesTotalG: Float,
    @SerializedName("fiber_g")
    val fiberG: Float,
    @SerializedName("sugar_g")
    val sugarG: Float
)
data class FoodSearchItem(
    val id: Int,
    val title: String,
    val image: String? = null,
    val imageType: String? = null
)
data class ParsedNutrition(
    val foodId: Int,
    val foodName: String,
    val calories: Float,
    val carbs: Float,
    val protein: Float,
    val fat: Float,
    val fiber: Float
) {
    fun scaled(porsi: Int): ParsedNutrition = copy(
        calories = calories * porsi,
        carbs    = carbs    * porsi,
        protein  = protein  * porsi,
        fat      = fat      * porsi,
        fiber    = fiber    * porsi
    )

    fun kaloriLabel(): String = when {
        calories < 200  -> "Rendah Kalori"
        calories < 400  -> "Kalori Sedang"
        calories < 600  -> "Sumber Energi Tinggi"
        else            -> "Sangat Tinggi Kalori"
    }
}