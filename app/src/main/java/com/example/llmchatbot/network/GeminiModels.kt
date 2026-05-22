package com.example.llmchatbot.network

/**
 * Request/response data classes mirroring the Gemini "generateContent" REST API.
 * Docs: https://ai.google.dev/api/generate-content
 *
 * The API expects a JSON body of shape:
 * {
 *   "contents": [
 *     { "role": "user", "parts": [ { "text": "..." } ] },
 *     { "role": "model", "parts": [ { "text": "..." } ] }
 *   ]
 * }
 */
data class GeminiRequest(
    val contents: List<Content>
)

data class Content(
    val role: String,           // "user" or "model"
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GeminiResponse(
    val candidates: List<Candidate>?,
    val error: ApiError?
)

data class Candidate(
    val content: Content?,
    val finishReason: String?
)

data class ApiError(
    val code: Int?,
    val message: String?
)
