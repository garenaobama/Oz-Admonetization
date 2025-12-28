package com.oz.android.utils.listener

/**
 * A sealed class that represents the result of an operation.
 * Can be either [Success] or [Failure].
 */
sealed class OzAdsResult<out T> {
    /**
     * Represents a successful operation.
     * @param data The result data.
     */
    data class Success<out T>(val data: T) : OzAdsResult<T>()

    /**
     * Represents a failed operation.
     * @param exception The exception that caused the failure.
     */
    data class Failure(val exception: Throwable) : OzAdsResult<Nothing>()
}