package com.oz.android.ads.oz_ads.ads_component.ads_inline

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.oz.android.ads.oz_ads.ads_component.AdState
import com.oz.android.ads.oz_ads.ads_component.AdsFormat
import com.oz.android.ads.oz_ads.ads_component.IOzAds
import com.oz.android.ads.oz_ads.ads_component.OzAds

/**
 * Abstract class để quản lý inline ads (banner, native) hiển thị cùng với content
 * InlineAds là một ViewGroup có thể được thêm vào layout như một child view
 * 
 * Hỗ trợ nhiều ad network (AdMob, Max, Meta...) nhưng hiện tại tối ưu cho AdMob
 * 
 * InlineAds chỉ hỗ trợ format: BANNER, NATIVE
 * Refresh time chỉ có trong inline format
 * 
 * Các implementation cụ thể sẽ extend class này và implement các abstract methods
 */
abstract class InlineAds @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), IOzAds {

    companion object {
        private const val TAG = "InlineAds"
        
        // Default refresh times (in milliseconds)
        private const val DEFAULT_REFRESH_TIME = 30_000L // 30 seconds
        private const val DEFAULT_MAX_REFRESH_TIME = 300_000L // 5 minutes
    }

    // Refresh time management (chỉ có trong inline format)
    private var refreshTime: Long = DEFAULT_REFRESH_TIME
    private var maxRefreshTime: Long = DEFAULT_MAX_REFRESH_TIME
    private var lastRefreshTime: Long = 0
    private var totalRefreshTime: Long = 0

    // Auto refresh handler
    private val refreshHandler = Handler(Looper.getMainLooper())
    private var refreshRunnable: Runnable? = null

    // Ad state
    private var isAdVisible = false

    // Delegate để sử dụng logic từ OzAds
    private val ozAdsDelegate = object : OzAds() {
        override fun isValidFormat(format: AdsFormat): Boolean {
            return format == AdsFormat.BANNER || format == AdsFormat.NATIVE
        }

        override fun getValidFormats(): List<AdsFormat> {
            return listOf(AdsFormat.BANNER, AdsFormat.NATIVE)
        }

        override fun hideAds() {
            this@InlineAds.hideAds()
        }

        override fun onLoadAd(key: String) {
            this@InlineAds.onLoadAd(key)
        }

        override fun onShowAds(key: String) {
            this@InlineAds.onShowAds(key)
        }

        override fun onDestroyAd(key: String) {
            this@InlineAds.onDestroyAd(key)
        }

        override fun isAdLoaded(): Boolean {
            // Not used in InlineAds, use isAdLoaded(key) instead
            return false
        }
    }

    init {
        // Inline ads chỉ hỗ trợ BANNER và NATIVE format
    }

    /**
     * Set ads format
     * Inline ads chỉ hỗ trợ BANNER và NATIVE
     */
    fun setAdsFormat(format: AdsFormat) {
        ozAdsDelegate.setAdsFormat(format)
    }

    /**
     * Get ads format hiện tại
     */
    fun getAdsFormat(): AdsFormat? = ozAdsDelegate.getAdsFormat()

    /**
     * Implementation của shouldShowAd() từ IOzAds
     */
    override fun shouldShowAd(): Boolean {
        return ozAdsDelegate.shouldShowAd() && isAdVisible
    }

    /**
     * Set trạng thái có nên hiển thị ad hay không
     */
    fun setShouldShowAd(shouldShow: Boolean) {
        ozAdsDelegate.setShouldShowAd(shouldShow)
        // InlineAds specific: show ad if shouldShow becomes true and ad is loaded
        if (shouldShow) {
            preloadKey?.let { key ->
                if (isAdLoaded(key)) {
                    showAds(key)
                }
            }
        }
    }

    /**
     * Implementation của setPreloadKey() từ IOzAds
     * Preload ad với key này
     */
    override fun setPreloadKey(key: String) {
        preloadKey = key
        ozAdsDelegate.setPreloadKey(key)
    }

    /**
     * Get state của ad với key
     * @param key Key để identify ad
     * @return AdState hiện tại, IDLE nếu chưa có
     */
    fun getAdState(key: String): AdState {
        return ozAdsDelegate.getAdState(key)
    }

    /**
     * Get preload key hiện tại
     */
    fun getPreloadKey(): String? {
        // Get from delegate's internal state
        // Since we can't access private fields, we'll track it ourselves
        return preloadKey
    }

    // Track preload key locally for InlineAds specific logic
    private var preloadKey: String? = null

    /**
     * Implementation của loadAd() từ IOzAds
     * Load ad với preload key đã set (nếu có)
     */
    override fun loadAd() {
        preloadKey?.let { key ->
            loadAd(key)
        } ?: run {
            Log.w(TAG, "No preload key set. Call setPreloadKey() first or use loadAd(key: String)")
        }
    }

    /**
     * Load ad với key cụ thể
     * Delegate to OzAds logic
     * @param key Key để identify ad cần load
     */
    fun loadAd(key: String) {
        ozAdsDelegate.loadAd(key)
    }

    /**
     * Implementation của showAds() từ IOzAds
     * Delegate to OzAds logic
     * @param key Key để identify ad cần show (đại diện cho placement)
     */
    override fun showAds(key: String) {
        ozAdsDelegate.showAds(key)
    }

    /**
     * Hide ads
     */
    protected abstract fun hideAds()

    /**
     * Set thời gian refresh ad (milliseconds)
     * @param timeInMillis Thời gian refresh tính bằng milliseconds
     */
    fun setRefreshTime(timeInMillis: Long) {
        if (timeInMillis <= 0) {
            Log.w(TAG, "Refresh time must be greater than 0")
            return
        }
        refreshTime = timeInMillis
        restartAutoRefresh()
    }

    /**
     * Set thời gian tối đa để refresh ad (milliseconds)
     * Sau thời gian này, ad sẽ không tự động refresh nữa
     * @param timeInMillis Thời gian tối đa tính bằng milliseconds
     */
    fun setMaxRefreshTime(timeInMillis: Long) {
        if (timeInMillis <= 0) {
            Log.w(TAG, "Max refresh time must be greater than 0")
            return
        }
        maxRefreshTime = timeInMillis
    }

    /**
     * Get thời gian refresh hiện tại
     * @return Thời gian refresh tính bằng milliseconds
     */
    fun getRefreshTime(): Long = refreshTime

    /**
     * Get thời gian tối đa refresh
     * @return Thời gian tối đa tính bằng milliseconds
     */
    fun getMaxRefreshTime(): Long = maxRefreshTime

    /**
     * Destroy ad và cleanup resources
     */
    fun destroy() {
        cancelAutoRefresh()
        isAdVisible = false
        
        // Delegate to OzAds destroy
        ozAdsDelegate.destroy()
    }

    /**
     * Destroy ad với key cụ thể
     * Các implementation cụ thể sẽ override method này
     * @param key Key của ad cần destroy
     */
    protected abstract fun onDestroyAd(key: String)

    /**
     * Abstract method để các implementation cụ thể load ad
     * @param key Key để identify ad cần load
     */
    protected abstract fun onLoadAd(key: String)

    /**
     * Abstract method để các implementation cụ thể show ad
     * @param key Key để identify ad cần show
     */
    protected abstract fun onShowAds(key: String)

    /**
     * Kiểm tra xem ad đã được load chưa
     * Abstract method để các implementation cụ thể implement
     * @param key Key để identify ad
     * @return true nếu ad đã load, false nếu chưa
     */
    protected abstract fun isAdLoaded(key: String): Boolean

    /**
     * Called khi ad được load thành công
     * Các implementation nên gọi method này sau khi load ad thành công
     * @param key Key của ad đã load thành công
     */
    protected fun onAdLoaded(key: String) {
        // Delegate to OzAds
        ozAdsDelegate.onAdLoaded(key)
        
        // Update refresh time tracking (inline specific)
        lastRefreshTime = System.currentTimeMillis()
        totalRefreshTime = 0
        
        // Bắt đầu auto refresh nếu chưa vượt quá max refresh time
        if (totalRefreshTime < maxRefreshTime && isAdVisible) {
            scheduleNextRefresh()
        }
    }

    /**
     * Called khi ad load thất bại
     * Các implementation nên gọi method này sau khi load ad thất bại
     * @param key Key của ad đã load thất bại
     */
    protected fun onAdLoadFailed(key: String) {
        // Delegate to OzAds
        ozAdsDelegate.onAdLoadFailed(key)
        
        // Retry sau một khoảng thời gian (inline specific)
        if (totalRefreshTime < maxRefreshTime && isAdVisible) {
            scheduleNextRefresh()
        }
    }

    /**
     * Called khi ad show thành công
     * Các implementation nên gọi method này sau khi show ad thành công
     * @param key Key của ad đã show thành công
     */
    protected fun onAdShown(key: String) {
        // Delegate to OzAds
        ozAdsDelegate.onAdShown(key)
    }

    /**
     * Called khi ad dismissed/closed
     * Các implementation nên gọi method này sau khi ad bị dismiss
     * @param key Key của ad đã bị dismiss
     */
    protected fun onAdDismissed(key: String) {
        // Delegate to OzAds
        ozAdsDelegate.onAdDismissed(key)
    }

    /**
     * Called khi ad show thất bại
     * Các implementation nên gọi method này sau khi show ad thất bại
     * @param key Key của ad đã show thất bại
     */
    protected fun onAdShowFailed(key: String) {
        // Delegate to OzAds
        ozAdsDelegate.onAdShowFailed(key)
    }

    /**
     * Get current showing key
     * @return Key đang được show, null nếu không có
     */
    fun getCurrentShowingKey(): String? = ozAdsDelegate.getCurrentShowingKey()

    /**
     * Schedule refresh ad sau một khoảng thời gian
     */
    private fun scheduleNextRefresh() {
        cancelAutoRefresh()
        
        refreshRunnable = Runnable {
            if (totalRefreshTime < maxRefreshTime && isAdVisible) {
                refreshAd()
            }
        }
        
        refreshHandler.postDelayed(refreshRunnable!!, refreshTime)
    }

    /**
     * Refresh ad (load lại ad mới)
     * Chỉ có trong inline format
     */
    fun refreshAd() {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - lastRefreshTime
        totalRefreshTime += elapsedTime
        lastRefreshTime = currentTime

        if (totalRefreshTime >= maxRefreshTime) {
            Log.d(TAG, "Max refresh time reached, stopping auto refresh")
            cancelAutoRefresh()
            return
        }

        Log.d(TAG, "Refreshing ad... (total time: ${totalRefreshTime}ms, max: ${maxRefreshTime}ms)")
        preloadKey?.let { key ->
            onDestroyAd(key)
            loadAd(key)
        }
    }

    /**
     * Restart auto refresh mechanism
     */
    private fun restartAutoRefresh() {
        preloadKey?.let { key ->
            if (isAdLoaded(key) && isAdVisible) {
                scheduleNextRefresh()
            }
        }
    }

    /**
     * Cancel auto refresh
     */
    private fun cancelAutoRefresh() {
        refreshRunnable?.let {
            refreshHandler.removeCallbacks(it)
            refreshRunnable = null
        }
    }

    /**
     * Pause ad (gọi trong onPause của Activity/Fragment)
     */
    fun pause() {
        isAdVisible = false
        cancelAutoRefresh()
        onPauseAd()
    }

    /**
     * Resume ad (gọi trong onResume của Activity/Fragment)
     */
    fun resume() {
        isAdVisible = true
        preloadKey?.let { key ->
            if (shouldShowAd()) {
                if (isAdLoaded(key)) {
                    showAds(key)
                } else {
                    loadAd(key)
                }
                if (totalRefreshTime < maxRefreshTime) {
                    scheduleNextRefresh()
                }
            }
        }
        onResumeAd()
    }

    /**
     * Override onAttachedToWindow để tự động load ad khi view được attach
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isAdVisible = true
        preloadKey?.let { key ->
            if (shouldShowAd() && !isAdLoaded(key)) {
                loadAd(key)
            }
        }
    }

    /**
     * Override onDetachedFromWindow để cleanup khi view được detach
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pause()
    }

    /**
     * Override onVisibilityChanged để handle visibility changes
     */
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        isAdVisible = visibility == VISIBLE
        
        preloadKey?.let { key ->
            if (isAdVisible && shouldShowAd()) {
                if (isAdLoaded(key)) {
                    showAds(key)
                } else {
                    loadAd(key)
                }
                if (totalRefreshTime < maxRefreshTime) {
                    scheduleNextRefresh()
                }
            } else {
                cancelAutoRefresh()
            }
        } ?: run {
            cancelAutoRefresh()
        }
    }

    /**
     * Abstract method để các implementation xử lý pause ad
     */
    protected abstract fun onPauseAd()

    /**
     * Abstract method để các implementation xử lý resume ad
     */
    protected abstract fun onResumeAd()

    /**
     * Default layout params cho InlineAds
     */
    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: LayoutParams?): LayoutParams {
        return LayoutParams(p)
    }

    /**
     * Override onMeasure để measure child views
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var maxHeight = 0
        var maxWidth = 0

        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec)
                maxWidth = maxOf(maxWidth, child.measuredWidth)
                maxHeight = maxOf(maxHeight, child.measuredHeight)
            }
        }

        setMeasuredDimension(
            resolveSize(maxWidth, widthMeasureSpec),
            resolveSize(maxHeight, heightMeasureSpec)
        )
    }

    /**
     * Override onLayout để layout child views
     */
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                child.layout(0, 0, child.measuredWidth, child.measuredHeight)
            }
        }
    }
}

