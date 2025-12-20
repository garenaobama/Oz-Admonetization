package com.oz.android.ads.oz_ads

import android.app.Activity
import com.oz.android.ads.network.admobs.AdMobManager
import com.oz.android.ads.network.admobs.AdMobResult
import com.oz.android.ads.network.admobs.IAdMobManager

/**
 * Business layer manager for handling ads
 * This layer coordinates between different ad networks
 * Following Dependency Inversion Principle - depends on abstraction (IAdMobManager)
 * Following Single Responsibility Principle - orchestrates ad network initialization
 */
class OzAdsManager private constructor(
    private val adMobManager: IAdMobManager
) {

    companion object {
        @Volatile
        private var instance: OzAdsManager? = null

        /**
         * Get singleton instance with default dependencies
         */
        fun getInstance(adMobManager: IAdMobManager = AdMobManager.getInstance()): OzAdsManager {
            return instance ?: synchronized(this) {
                instance ?: OzAdsManager(adMobManager).also { instance = it }
            }
        }

        /**
         * Reset instance (useful for testing)
         */
        fun resetInstance() {
            synchronized(this) {
                instance = null
            }
        }
    }

    /**
     * Initialize all ad networks
     * Currently initializes AdMob only
     * 
     * @param activity The activity context for initialization
     * @param adMobAppId Optional AdMob app ID (will use manifest value if null)
     * @return Result indicating success or failure
     */
    suspend fun init(
        activity: Activity,
        adMobAppId: String? = null
    ): OzAdsResult<Unit> {
        return when (val result = adMobManager.initialize(activity, adMobAppId)) {
            is AdMobResult.Success -> OzAdsResult.Success(Unit)
            is AdMobResult.Error -> OzAdsResult.Error(result.exception)
        }
    }

    /**
     * Check if all ad networks are initialized
     */
    fun isInitialized(): Boolean {
        return adMobManager.isInitialized()
    }
}

/**
 * Sealed class for OzAds operation results
 * Following Clean Architecture error handling pattern
 */
sealed class OzAdsResult<out T> {
    data class Success<T>(val data: T) : OzAdsResult<T>()
    data class Error(val exception: Throwable) : OzAdsResult<Nothing>()
}
