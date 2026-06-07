package com.example.data.remote

import com.squareup.moshi.Json
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * vivo AI 文生图 API 响应数据类
 */
data class VivoImageResponse(
    val code: Int,
    @Json(name = "message") val message: String,
    val data: VivoImageData?
)

data class VivoImageData(
    val images: List<VivoImageItem>?,
    val usage: VivoImageUsage?
)

data class VivoImageItem(
    val url: String?
)

data class VivoImageUsage(
    @Json(name = "image_count") val imageCount: Int?
)

/**
 * vivo AI 文生图 API 请求数据类
 */
data class VivoImageRequest(
    val model: String,
    val prompt: String,
    val parameters: VivoImageParameters? = null
)

data class VivoImageParameters(
    val size: String? = "720x1280"
)

/**
 * vivo AI 文生图 API 接口定义
 */
interface VivoImageApi {
    @POST("api/v1/image_generation")
    suspend fun generateImage(
        @Header("Authorization") authorization: String,
        @Query("request_id") requestId: String,
        @Query("module") module: String,
        @Query("system_time") systemTime: Long,
        @Body request: VivoImageRequest
    ): VivoImageResponse
}
