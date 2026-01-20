package com.builder.data.util

import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.math.min
import kotlin.math.pow

/**
 * Retry configuration for API calls.
 */
data class RetryConfig(
    val maxAttempts: Int = 3,
    val initialDelayMs: Long = 1000,
    val maxDelayMs: Long = 10000,
    val backoffMultiplier: Double = 2.0,
    val retryOn: (Exception) -> Boolean = { it.isRetryable() }
)

/**
 * Default retry configuration for network calls.
 */
val DefaultRetryConfig = RetryConfig()

/**
 * Executes a suspend function with retry logic and exponential backoff.
 *
 * @param config Retry configuration
 * @param operation The operation to retry
 * @return Result of the operation
 */
suspend fun <T> withRetry(
    config: RetryConfig = DefaultRetryConfig,
    operation: suspend () -> T
): T {
    var lastException: Exception? = null
    var currentDelay = config.initialDelayMs

    repeat(config.maxAttempts) { attempt ->
        try {
            return operation()
        } catch (e: Exception) {
            lastException = e

            if (!config.retryOn(e)) {
                Timber.d("Exception not retryable: ${e.message}")
                throw e
            }

            if (attempt < config.maxAttempts - 1) {
                Timber.w("Attempt ${attempt + 1}/${config.maxAttempts} failed: ${e.message}. Retrying in ${currentDelay}ms...")
                delay(currentDelay)
                currentDelay = min(
                    (currentDelay * config.backoffMultiplier).toLong(),
                    config.maxDelayMs
                )
            }
        }
    }

    Timber.e("All ${config.maxAttempts} attempts failed")
    throw lastException ?: Exception("Retry failed with unknown error")
}

/**
 * Executes a suspend function with retry logic, returning a Result.
 */
suspend fun <T> withRetryResult(
    config: RetryConfig = DefaultRetryConfig,
    operation: suspend () -> T
): Result<T> {
    return try {
        Result.success(withRetry(config, operation))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * Determines if an exception is retryable (typically network errors).
 */
fun Exception.isRetryable(): Boolean {
    return when (this) {
        is java.net.SocketTimeoutException -> true
        is java.net.UnknownHostException -> true
        is java.net.ConnectException -> true
        is java.io.IOException -> true
        is retrofit2.HttpException -> {
            // Retry on server errors (5xx) and rate limiting (429)
            val code = this.code()
            code in 500..599 || code == 429
        }
        else -> false
    }
}

/**
 * Extension to retry a Result-returning function.
 */
suspend fun <T> retryOnFailure(
    config: RetryConfig = DefaultRetryConfig,
    operation: suspend () -> Result<T>
): Result<T> {
    var lastResult: Result<T> = Result.failure(Exception("No attempts made"))
    var currentDelay = config.initialDelayMs

    repeat(config.maxAttempts) { attempt ->
        lastResult = operation()

        if (lastResult.isSuccess) {
            return lastResult
        }

        val exception = lastResult.exceptionOrNull()
        if (exception != null && !config.retryOn(exception)) {
            Timber.d("Exception not retryable: ${exception.message}")
            return lastResult
        }

        if (attempt < config.maxAttempts - 1) {
            Timber.w("Attempt ${attempt + 1}/${config.maxAttempts} failed. Retrying in ${currentDelay}ms...")
            delay(currentDelay)
            currentDelay = min(
                (currentDelay * config.backoffMultiplier).toLong(),
                config.maxDelayMs
            )
        }
    }

    return lastResult
}
