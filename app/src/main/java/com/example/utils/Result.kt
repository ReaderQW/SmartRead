package com.example.utils

/**
 * 通用的结果包装类
 * 用于统一处理成功/失败情况
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data

    fun exceptionOrNull(): Exception? = (this as? Error)?.exception

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(exception)
        is Loading -> Loading
    }

    inline fun <R> mapCatching(transform: (T) -> R): Result<R> = when (this) {
        is Success -> try {
            Success(transform(data))
        } catch (e: Exception) {
            Error(e)
        }
        is Error -> Error(exception)
        is Loading -> Loading
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (Exception) -> Unit): Result<T> {
        if (this is Error) action(exception)
        return this
    }

    inline fun onLoading(action: () -> Unit): Result<T> {
        if (this is Loading) action()
        return this
    }

    inline fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (Exception) -> R
    ): R = when (this) {
        is Success -> onSuccess(data)
        is Error -> onFailure(exception)
        is Loading -> throw IllegalStateException("Cannot fold Loading state")
    }

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun <T> failure(exception: Exception): Result<T> = Error(exception)
        fun <T> loading(): Result<T> = Loading
    }
}

// 扩展函数方便创建
fun <T> success(data: T): Result<T> = Result.Success(data)
fun <T> error(exception: Exception): Result<T> = Result.Error(exception)
fun <T> loading(): Result<T> = Result.Loading as Result<T>

// catch异常的包装
inline fun <T> runCatchingResult(block: () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: Exception) {
    Result.Error(e)
}

// suspend版本
suspend inline fun <T> runCatchingSuspend(crossinline block: suspend () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: Exception) {
    Result.Error(e)
}