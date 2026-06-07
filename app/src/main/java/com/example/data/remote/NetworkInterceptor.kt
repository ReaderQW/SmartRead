package com.example.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 网络请求重试拦截器
 * 对临时性网络错误和可重试 HTTP 状态码进行指数退避重试
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 100L,
    private val maxDelayMs: Long = 10000L
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var delayMs = initialDelayMs
        var lastException: IOException? = null

        for (attempt in 0..maxRetries) {
            try {
                val response = chain.proceed(chain.request())
                if (isRetryable(response.code) && attempt < maxRetries) {
                    response.close()
                    Thread.sleep(delayMs)
                    delayMs = minOf(delayMs * 2, maxDelayMs)
                    continue
                }
                return response
            } catch (e: SocketTimeoutException) {
                lastException = e
                if (attempt < maxRetries) {
                    Thread.sleep(delayMs)
                    delayMs = minOf(delayMs * 2, maxDelayMs)
                }
            } catch (e: UnknownHostException) {
                // DNS 解析失败不重试
                throw e
            } catch (e: IOException) {
                lastException = e
                if (attempt < maxRetries) {
                    Thread.sleep(delayMs)
                    delayMs = minOf(delayMs * 2, maxDelayMs)
                }
            }
        }

        throw lastException ?: IOException("Max retries ($maxRetries) exceeded")
    }

    private fun isRetryable(statusCode: Int): Boolean = when (statusCode) {
        408, 429, 500, 502, 503, 504 -> true
        else -> false
    }
}

/**
 * HTTP 头通用拦截器
 * 为所有请求添加标准头部和请求追踪 ID
 */
class HeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestWithHeaders = originalRequest.newBuilder()
            .header("User-Agent", "SmartRead/1.0")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("X-Request-ID", "${System.currentTimeMillis()}-${System.nanoTime()}")
            .build()

        return chain.proceed(requestWithHeaders)
    }
}

/**
 * 请求/响应日志拦截器
 * 记录请求 URL、耗时和响应状态码
 */
class RequestLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()

        return try {
            val response = chain.proceed(request)
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.d("HTTP", "→ ${request.method} ${request.url} (${duration}ms) ← ${response.code}")
            response
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.e("HTTP", "✗ ${request.method} ${request.url} (${duration}ms) - ${e.message}")
            throw e
        }
    }
}

/**
 * 网络超时配置常量
 */
object NetworkConfig {
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L
}