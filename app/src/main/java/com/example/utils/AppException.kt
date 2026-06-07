package com.example.utils

import java.io.IOException

/**
 * 应用级异常类
 * 分类管理所有可能的错误
 */
sealed class AppException(message: String? = null, cause: Throwable? = null) :
    Exception(message, cause) {

    // 网络错误
    data class NetworkException(
        val message_: String,
        val statusCode: Int? = null,
        val cause_: Throwable? = null
    ) : AppException(message_, cause_)

    // 数据库错误
    data class DatabaseException(
        val message_: String,
        val cause_: Throwable? = null
    ) : AppException(message_, cause_)

    // 数据验证错误
    data class ValidationException(
        val message_: String,
        val fieldName: String? = null,
        val cause_: Throwable? = null
    ) : AppException(message_, cause_)

    // 数据未找到
    data class NotFoundException(
        val resourceType: String,
        val resourceId: String
    ) : AppException("$resourceType not found: $resourceId")

    // 业务逻辑错误
    data class BusinessException(
        val message_: String,
        val code: String? = null,
        val cause_: Throwable? = null
    ) : AppException(message_, cause_)

    // 权限错误
    data class PermissionException(
        val message_: String
    ) : AppException(message_)

    // 未知错误
    data class UnknownException(
        val message_: String,
        val cause_: Throwable? = null
    ) : AppException(message_, cause_)
}

/**
 * 异常分类和转换工具
 */
object ExceptionHandler {

    fun classify(throwable: Throwable): AppException = when (throwable) {
        is AppException -> throwable
        is IOException -> AppException.NetworkException(
            throwable.message ?: "Network error",
            cause_ = throwable
        )
        is IllegalArgumentException -> AppException.ValidationException(
            throwable.message ?: "Invalid argument",
            cause_ = throwable
        )
        else -> AppException.UnknownException(
            throwable.message ?: "Unknown error",
            cause_ = throwable
        )
    }

    fun getMessage(exception: AppException): String = when (exception) {
        is AppException.NetworkException -> exception.message_
            ?: "Network error occurred (${exception.statusCode})"
        is AppException.DatabaseException -> exception.message_ ?: "Database error"
        is AppException.ValidationException -> exception.message_ ?: "Invalid input"
        is AppException.NotFoundException -> "Resource not found"
        is AppException.BusinessException -> exception.message_ ?: "Operation failed"
        is AppException.PermissionException -> exception.message_ ?: "Permission denied"
        is AppException.UnknownException -> exception.message_ ?: "Unknown error"
    }

    fun isRetryable(exception: AppException): Boolean = when (exception) {
        is AppException.NetworkException -> {
            val code = exception.statusCode
            code == null || code in 408..429 || code in 500..599
        }
        is AppException.DatabaseException -> true
        else -> false
    }
}