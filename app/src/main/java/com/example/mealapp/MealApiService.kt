package com.example.mealapp

import retrofit2.http.GET
import retrofit2.http.Query

interface MealApiService {
    @GET("random.php")
    suspend fun getRandomMeal(): MealResponse

    @GET("search.php/")
    suspend fun searchMeals(@Query("s") query: String): MealResponse

    @GET("categories.php")
    suspend fun getCategories(): CategoriesResponse

    @GET("filter.php")
    suspend fun getMealsByCategory(@Query("c") category: String): MealsResponse


}



