package com.oz.android.ads.oz_ads.ads_component.ads_inline.admob

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import com.google.android.gms.ads.nativead.NativeAdView
import com.oz.android.ads.network.admobs.ads_component.native_advanced.AdmobNativeAdvanced
import com.oz.android.ads.oz_ads.ads_component.AdsFormat
import com.oz.android.ads.oz_ads.ads_component.ads_inline.InlineAds
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation cụ thể của InlineAds cho AdMob Native
 * Chỉ xử lý NATIVE format
 */
class OzAdmobNativeAd @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : InlineAds(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "OzAdmobNativeAd"
    }

    // Map key -> AdmobNativeAdvanced instance
    private val nativeAds = ConcurrentHashMap<String, AdmobNativeAdvanced>()

    // Map key -> adUnitId
    private val adUnitIds = ConcurrentHashMap<String, String>()

    // Map key -> NativeAdView
    private val nativeAdViews = ConcurrentHashMap<String, NativeAdView>()

    init {
        // Set format to NATIVE
        setAdsFormat(AdsFormat.NATIVE)
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
     * Set NativeAdView cho một key
     * @param key Key để identify placement
     * @param nativeAdView NativeAdView đã được setup
     */
    fun setNativeAdView(key: String, nativeAdView: NativeAdView) {
        nativeAdViews[key] = nativeAdView
        Log.d(TAG, "NativeAdView set for key: $key")
    }

    /**
     * Get ad unit ID cho một key
     * @param key Key để identify placement
     * @return Ad unit ID, null nếu chưa được set
     */
    fun getAdUnitId(key: String): String? = adUnitIds[key]

    /**
     * Implementation của onLoadAd từ InlineAds
     * Load native ad
     */
    override fun onLoadAd(key: String) {
        val adUnitId = adUnitIds[key]
        if (adUnitId == null) {
            Log.e(TAG, "Ad unit ID not set for key: $key")
            onAdLoadFailed(key)
            return
        }

        Log.d(TAG, "Loading native ad for key: $key")

        // Destroy old native nếu có
        nativeAds[key]?.destroy()

        // Tạo native instance mới
        val nativeAd = AdmobNativeAdvanced(context, adUnitId)
        nativeAds[key] = nativeAd

        // Load native
        nativeAd.load()

        // Note: onAdLoaded(key) cần được gọi khi native load thành công
        // Có thể track qua callback hoặc check state sau một khoảng thời gian
    }

    /**
     * Implementation của onShowAds từ InlineAds
     * Show native ad
     */
    override fun onShowAds(key: String) {
        val nativeAd = nativeAds[key]
        val nativeAdView = nativeAdViews[key]

        if (nativeAd == null) {
            Log.e(TAG, "Native ad not found for key: $key")
            onAdShowFailed(key)
            return
        }

        if (nativeAdView == null) {
            Log.e(TAG, "NativeAdView not set for key: $key")
            onAdShowFailed(key)
            return
        }

        // Check if native ad is loaded
        if (!nativeAd.isAdLoaded()) {
            Log.w(TAG, "Native ad not loaded yet for key: $key")
            onAdShowFailed(key)
            return
        }

        // Show native vào chính InlineAds (this là ViewGroup)
        nativeAd.show(this, nativeAdView)

        // Call onAdShown để notify parent
        onAdShown(key)
        Log.d(TAG, "Native ad shown for key: $key")
    }

    /**
     * Implementation của hideAds từ InlineAds
     */
    override fun hideAds() {
        // Remove all child views
        removeAllViews()
        Log.d(TAG, "Native ads hidden")
    }

    /**
     * Implementation của isAdLoaded từ InlineAds
     * Check native ad đã loaded chưa
     */
    override fun isAdLoaded(key: String): Boolean {
        // Check directly from AdmobNativeAdvanced
        return nativeAds[key]?.isAdLoaded() ?: false
    }

    /**
     * Implementation của onDestroyAd từ InlineAds
     * Destroy native ad
     */
    override fun onDestroyAd(key: String) {
        Log.d(TAG, "Destroying native ad for key: $key")

        // Destroy native
        nativeAds[key]?.destroy()
        nativeAds.remove(key)
        nativeAdViews.remove(key)

        // Remove all views
        removeAllViews()
    }

    /**
     * Implementation của onPauseAd từ InlineAds
     */
    override fun onPauseAd() {
        // Native ads don't need pause/resume
        Log.d(TAG, "Native ads paused (no-op)")
    }

    /**
     * Implementation của onResumeAd từ InlineAds
     */
    override fun onResumeAd() {
        // Native ads don't need pause/resume
        Log.d(TAG, "Native ads resumed (no-op)")
    }

    /**
     * Override destroy để cleanup tất cả
     */
    override fun destroy() {
        Log.d(TAG, "Destroying all native ads")

        // Destroy tất cả native ads
        nativeAds.values.forEach { it.destroy() }
        nativeAds.clear()

        // Clear all maps
        adUnitIds.clear()
        nativeAdViews.clear()

        // Remove all views
        removeAllViews()

        super.destroy()
    }
}

