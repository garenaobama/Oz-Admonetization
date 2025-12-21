package com.oz.android.ads.oz_ads.ads_component.ads_inline.admob

import AdmobBanner
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.LoadAdError
import com.oz.android.ads.network.admobs.ads_component.OzAdmobListener
import com.oz.android.ads.oz_ads.ads_component.AdsFormat
import com.oz.android.ads.oz_ads.ads_component.ads_inline.InlineAds
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation cụ thể của InlineAds cho AdMob Banner
 * Chỉ xử lý BANNER format
 */
class OzAdmobBannerAd @JvmOverloads constructor(
    context: Context,
) : InlineAds<AdmobBanner>(context) {

    companion object {
        private const val TAG = "OzAdmobBannerAd"
    }

    // Map key -> adUnitId
    private val adUnitIds = ConcurrentHashMap<String, String>()

    init {
        // Set format to BANNER by default for this specific class
        setAdsFormat(AdsFormat.BANNER)
    }

    /**
     * Set ad unit ID cho một key
     * @param key Key để identify placement
     * @param adUnitId Ad unit ID từ AdMob
     */
    fun setAdUnitId(key: String, adUnitId: String) {
        adUnitIds[key] = adUnitId
        Log.d(TAG, "Ad unit ID set for key: $key -> $adUnitId")
    }

    /**
     * Get ad unit ID cho một key
     * @param key Key để identify placement
     * @return Ad unit ID, null nếu chưa được set
     */
    fun getAdUnitId(key: String): String? = adUnitIds[key]

    override fun createAd(key: String): AdmobBanner? {
        val adUnitId = adUnitIds[key]
        if (adUnitId == null) {
            Log.e(TAG, "Ad unit ID not set for key: $key")
            return null
        }

        val listener = object : OzAdmobListener<AdmobBanner>() {
            override fun onAdLoaded(ad: AdmobBanner) {
                // Pass the loaded ad object to the parent
                this@OzAdmobBannerAd.onAdLoaded(key, ad)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                // Notify parent about the failure
                this@OzAdmobBannerAd.onAdLoadFailed(key, error.message)
            }
        }

        return AdmobBanner(context, adUnitId, listener)
    }

    override fun onLoadAd(key: String, ad: AdmobBanner) {
        Log.d(TAG, "Loading banner ad for key: $key")
        ad.load()
    }

    override fun onShowAds(key: String, ad: AdmobBanner) {
        Log.d(TAG, "Showing banner ad for key: $key")
        // Show banner in this ViewGroup
        ad.show(this)
        // Notify parent that the ad has been shown
        onAdShown(key)
    }

    override fun hideAds() {
        // Remove all child views to hide the ad
        removeAllViews()
        Log.d(TAG, "Banner ads hidden")
    }

    override fun destroyAd(ad: AdmobBanner) {
        Log.d(TAG, "Destroying banner ad")
        ad.destroy()
    }

    override fun onPauseAd() {
        Log.d(TAG, "Pausing all banner ads")
        adStore.values.forEach { it.pause() }
    }

    override fun onResumeAd() {
        Log.d(TAG, "Resuming all banner ads")
        adStore.values.forEach { it.resume() }
    }
}



