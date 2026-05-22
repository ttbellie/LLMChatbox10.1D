package com.example.llmchatbot.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GeminiApi {

    /**
     * Calls Gemini's generateContent endpoint.
     * The model name (e.g. "gemini-1.5-flash") is path-injected so we can swap models easily.
     */
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}
