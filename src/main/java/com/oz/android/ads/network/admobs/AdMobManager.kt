package com.oz.android.ads.network.admobs

import android.app.Activity
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manager class for handling AdMob ads
 * Following Single Responsibility Principle - only handles AdMob initialization
 */
class AdMobManager private constructor() : IAdMobManager {

    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    
    @Volatile
    private var currentActivity: Activity? = null

    companion object {
        const val TEST_DEVICE_HASHED_ID = "test"

        @Volatile
        private var instance: AdMobManager? = null

        /**
         * Get singleton instance
         * Thread-safe lazy initialization
         */
        fun getInstance(): AdMobManager {
            return instance ?: synchronized(this) {
                instance ?: AdMobManager().also { instance = it }
            }
        }
    }

    override suspend fun initialize(
        activity: Activity,
        appId: String?
    ): AdMobResult<Unit> = withContext(Dispatchers.IO) {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return@withContext AdMobResult.Success(Unit)
        }

        try {
            // Store activity reference for consent manager
            currentActivity = activity

            // Set your test devices
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf(TEST_DEVICE_HASHED_ID))
                    .build()
            )

            // Initialize the Google Mobile Ads SDK on a background thread
            // App ID is read from AndroidManifest.xml if not provided
            MobileAds.initialize(activity) {}

            AdMobResult.Success(Unit)
        } catch (e: Exception) {
            isMobileAdsInitializeCalled.set(false)
            AdMobResult.Error(e)
        }
    }

    override fun isInitialized(): Boolean {
        return isMobileAdsInitializeCalled.get()
    }

    /**
     * Get the current activity reference (for consent manager)
     * @return Activity if available, null otherwise
     */
    fun getCurrentActivity(): Activity? = currentActivity
}
