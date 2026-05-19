package com.example.smartread.data.vector

import retrofit2.http.Body
import retrofit2.http.POST
import javax.inject.Inject

interface EmbeddingApi {
    @POST("v1/embeddings")
    suspend fun embed(@Body request: EmbeddingRequest): EmbeddingResponse
}

data class EmbeddingRequest(val text: String)
data class EmbeddingResponse(val vector: List<Float>)

class EmbeddingClient @Inject constructor(
    private val api: EmbeddingApi
) {
    suspend fun embed(text: String): List<Float> = api.embed(EmbeddingRequest(text)).vector
}

