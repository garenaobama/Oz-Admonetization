package com.oz.android.ads.oz_ads.ads_component.ads_inline.admob

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import com.oz.android.ads.network.admobs.ads_component.banner.AdmobBanner
import com.oz.android.ads.oz_ads.ads_component.AdsFormat
import com.oz.android.ads.oz_ads.ads_component.ads_inline.InlineAds
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation cụ thể của InlineAds cho AdMob Banner
 * Chỉ xử lý BANNER format
 */
class OzAdmobBannerAd @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : InlineAds(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "OzAdmobBannerAd"
    }

    // Map key -> AdmobBanner instance
    private val bannerAds = ConcurrentHashMap<String, AdmobBanner>()

    // Map key -> adUnitId
    private val adUnitIds = ConcurrentHashMap<String, String>()

    init {
        // Set format to BANNER
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

    /**
     * Implementation của onLoadAd từ InlineAds
     * Load banner ad
     */
    override fun onLoadAd(key: String) {
        val adUnitId = adUnitIds[key]
        if (adUnitId == null) {
            Log.e(TAG, "Ad unit ID not set for key: $key")
            onAdLoadFailed(key)
            return
        }

        Log.d(TAG, "Loading banner ad for key: $key")

        // Destroy old banner nếu có
        bannerAds[key]?.destroy()

        // Tạo banner instance mới
        val banner = AdmobBanner(context, adUnitId)
        bannerAds[key] = banner

        // Load banner
        banner.load()

        // Note: onAdLoaded(key) cần được gọi khi banner load thành công
        // Có thể track qua callback hoặc check state sau một khoảng thời gian
    }

    /**
     * Implementation của onShowAds từ InlineAds
     * Show banner ad
     */
    override fun onShowAds(key: String) {
        val banner = bannerAds[key]
        if (banner == null) {
            Log.e(TAG, "Banner ad not found for key: $key")
            onAdShowFailed(key)
            return
        }

        // Show banner vào chính InlineAds (this là ViewGroup)
        banner.show(this)

        // Call onAdShown để notify parent
        onAdShown(key)
        Log.d(TAG, "Banner ad shown for key: $key")
    }

    /**
     * Implementation của hideAds từ InlineAds
     */
    override fun hideAds() {
        // Remove all child views
        removeAllViews()
        Log.d(TAG, "Banner ads hidden")
    }

    /**
     * Implementation của isAdLoaded từ InlineAds
     * Check banner ad đã loaded chưa
     */
    override fun isAdLoaded(key: String): Boolean {
        // Check if banner exists and was shown (has child views)
        return bannerAds[key] != null && childCount > 0
    }

    /**
     * Implementation của onDestroyAd từ InlineAds
     * Destroy banner ad
     */
    override fun onDestroyAd(key: String) {
        Log.d(TAG, "Destroying banner ad for key: $key")

        // Destroy banner
        bannerAds[key]?.destroy()
        bannerAds.remove(key)

        // Remove all views
        removeAllViews()
    }

    /**
     * Implementation của onPauseAd từ InlineAds
     */
    override fun onPauseAd() {
        // Pause tất cả banner ads
        bannerAds.values.forEach { it.pause() }
        Log.d(TAG, "All banner ads paused")
    }

    /**
     * Implementation của onResumeAd từ InlineAds
     */
    override fun onResumeAd() {
        // Resume tất cả banner ads
        bannerAds.values.forEach { it.resume() }
        Log.d(TAG, "All banner ads resumed")
    }

    /**
     * Override destroy để cleanup tất cả
     */
    override fun destroy() {
        Log.d(TAG, "Destroying all banner ads")

        // Destroy tất cả banner ads
        bannerAds.values.forEach { it.destroy() }
        bannerAds.clear()

        // Clear ad unit IDs
        adUnitIds.clear()

        // Remove all views
        removeAllViews()

        super.destroy()
    }
}

