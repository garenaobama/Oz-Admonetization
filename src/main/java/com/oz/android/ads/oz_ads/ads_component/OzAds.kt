package com.oz.android.ads.oz_ads.ads_component

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Abstract class chung cho tất cả các loại OzAds
 * Implement interface IOzAds và cung cấp các tính năng chung
 * 
 * Quản lý preload ads bằng key (key đại diện cho placement, không phải ad id)
 * Ads có 4 state: IDLE, LOADING, LOADED, SHOWING
 * 
 * Các implementation cụ thể (như AdmobInlineAds, AdmobOverlayAds) sẽ extend class này
 */
abstract class OzAds : IOzAds {

    companion object {
        private const val TAG = "OzAds"
    }

    // Ads format
    protected var adsFormat: AdsFormat? = null
        private set

    // Should show ad flag
    protected var shouldShow: Boolean = true
        private set

    // Preloaded ads state management (key -> state)
    private val adStates = ConcurrentHashMap<String, AdState>()

    // Pending show callbacks (key -> callback)
    private val pendingShows = ConcurrentHashMap<String, () -> Unit>()

    // Current showing key
    private var currentShowingKey: String? = null

    /**
     * Set ads format
     * @param format AdsFormat để định nghĩa loại ad
     * @throws IllegalArgumentException nếu format không hợp lệ cho loại ads này
     */
    fun setAdsFormat(format: AdsFormat) {
        // Validate format dựa trên loại ads (inline hoặc overlay)
        if (!isValidFormat(format)) {
            throw IllegalArgumentException(
                "Format $format is not valid for ${this::class.simpleName}. " +
                "Valid formats: ${getValidFormats().joinToString()}"
            )
        }
        
        adsFormat = format
        Log.d(TAG, "Ads format set to: $format")
    }

    /**
     * Get ads format hiện tại
     * @return AdsFormat hiện tại, null nếu chưa được set
     */
    fun getAdsFormat(): AdsFormat? = adsFormat

    /**
     * Kiểm tra xem format có hợp lệ cho loại ads này không
     * Các implementation cụ thể sẽ override method này
     * @param format Format cần kiểm tra
     * @return true nếu hợp lệ, false nếu không
     */
    protected abstract fun isValidFormat(format: AdsFormat): Boolean

    /**
     * Get danh sách các format hợp lệ cho loại ads này
     * Các implementation cụ thể sẽ override method này
     * @return List các format hợp lệ
     */
    protected abstract fun getValidFormats(): List<AdsFormat>

    /**
     * Implementation của shouldShowAd() từ interface
     * @return true nếu nên hiển thị, false nếu không
     */
    override fun shouldShowAd(): Boolean {
        return shouldShow
    }

    /**
     * Set trạng thái có nên hiển thị ad hay không
     * @param shouldShow true để hiển thị, false để ẩn
     */
    fun setShouldShowAd(shouldShow: Boolean) {
        this.shouldShow = shouldShow
        if (!shouldShow) {
            hideAds()
        }
        // Note: Showing ads when shouldShow becomes true should be handled by implementation
        // as it requires a key parameter
    }

    /**
     * Implementation của setPreloadKey() từ interface
     * Preload ad với key này
     * @param key Key để identify ad cần preload (đại diện cho placement)
     */
    override fun setPreloadKey(key: String) {
        if (adsFormat == null) {
            Log.w(TAG, "Ads format not set. Call setAdsFormat() first")
            return
        }
        
        // Set state to IDLE nếu chưa có
        adStates.putIfAbsent(key, AdState.IDLE)
        
        // Preload ad
        loadAd(key)
        Log.d(TAG, "Preload key set to: $key")
    }

    /**
     * Get state của ad với key
     * @param key Key để identify ad
     * @return AdState hiện tại, IDLE nếu chưa có
     */
    fun getAdState(key: String): AdState {
        return adStates.getOrDefault(key, AdState.IDLE)
    }

    /**
     * Implementation của loadAd() từ interface
     * Load ad với preload key đã set (nếu có)
     */
    override fun loadAd() {
        // Nếu có preload key, load với key đó
        // Nếu không, implementation cụ thể sẽ handle
        Log.d(TAG, "loadAd() called without key")
    }

    /**
     * Load ad với key cụ thể
     * @param key Key để identify ad cần load
     */
    fun loadAd(key: String) {
        if (adsFormat == null) {
            Log.w(TAG, "Ads format not set. Call setAdsFormat() first")
            setAdState(key, AdState.IDLE)
            return
        }

        val currentState = getAdState(key)
        
        when (currentState) {
            AdState.IDLE -> {
                // Idle: call loadAd() (load from mediation, not recursive)
                Log.d(TAG, "Loading ad for key: $key (state: IDLE)")
                setAdState(key, AdState.LOADING)
                onLoadAd(key)
            }
            AdState.LOADING -> {
                // Loading: return cause it's loading
                Log.d(TAG, "Ad already loading for key: $key")
                return
            }
            AdState.SHOWING -> {
                // Showing: load a new one
                Log.d(TAG, "Ad is showing for key: $key, loading a new one")
                setAdState(key, AdState.LOADING)
                onLoadAd(key)
            }
            AdState.LOADED -> {
                // Loaded: return because it is loaded
                Log.d(TAG, "Ad already loaded for key: $key")
                return
            }
        }
    }

    /**
     * Implementation của showAds() từ interface
     * @param key Key để identify ad cần show (đại diện cho placement)
     */
    override fun showAds(key: String) {
        if (!shouldShowAd()) {
            Log.d(TAG, "Should not show ad, skipping showAds() for key: $key")
            setAdState(key, AdState.IDLE)
            return
        }

        val currentState = getAdState(key)
        
        when (currentState) {
            AdState.IDLE -> {
                // Idle: call to loadThenShow() method
                Log.d(TAG, "Showing ad for key: $key (state: IDLE -> loadThenShow)")
                setAdState(key, AdState.LOADING)
                // Set pending show
                pendingShows[key] = {
                    setAdState(key, AdState.SHOWING)
                    currentShowingKey = key
                    onShowAds(key)
                }
                onLoadAd(key)
            }
            AdState.LOADING -> {
                // Loading: create a pendingShow that show ads once it done
                Log.d(TAG, "Ad loading for key: $key, setting pending show")
                pendingShows[key] = {
                    setAdState(key, AdState.SHOWING)
                    currentShowingKey = key
                    onShowAds(key)
                }
            }
            AdState.SHOWING -> {
                // Showing: return because it is showing
                Log.d(TAG, "Ad already showing for key: $key")
                return
            }
            AdState.LOADED -> {
                // Loaded: call showAd()
                Log.d(TAG, "Showing ad for key: $key (state: LOADED)")
                setAdState(key, AdState.SHOWING)
                currentShowingKey = key
                onShowAds(key)
            }
        }
    }

    /**
     * Hide ads
     * Các implementation cụ thể sẽ override method này
     */
    protected abstract fun hideAds()

    /**
     * Abstract method để các implementation cụ thể load ad từ mediation
     * @param key Key để identify ad cần load
     */
    protected abstract fun onLoadAd(key: String)

    /**
     * Abstract method để các implementation cụ thể show ad
     * @param key Key để identify ad cần show
     */
    protected abstract fun onShowAds(key: String)

    /**
     * Called khi ad load thành công
     * Các implementation nên gọi method này sau khi load ad thành công
     * @param key Key của ad đã load thành công
     */
    protected fun onAdLoaded(key: String) {
        val currentState = getAdState(key)
        if (currentState == AdState.LOADING) {
            setAdState(key, AdState.LOADED)
            Log.d(TAG, "Ad loaded successfully for key: $key")
            
            // Check if there's a pending show
            pendingShows.remove(key)?.invoke()
        }
    }

    /**
     * Called khi ad load thất bại
     * Các implementation nên gọi method này sau khi load ad thất bại
     * @param key Key của ad đã load thất bại
     */
    protected fun onAdLoadFailed(key: String) {
        Log.e(TAG, "Ad load failed for key: $key, setting state to IDLE")
        setAdState(key, AdState.IDLE)
        pendingShows.remove(key)
    }

    /**
     * Called khi ad show thành công
     * Các implementation nên gọi method này sau khi show ad thành công
     * @param key Key của ad đã show thành công
     */
    protected fun onAdShown(key: String) {
        Log.d(TAG, "Ad shown successfully for key: $key")
        // State đã được set thành SHOWING trong showAds()
    }

    /**
     * Called khi ad dismissed/closed
     * Các implementation nên gọi method này sau khi ad bị dismiss
     * @param key Key của ad đã bị dismiss
     */
    protected fun onAdDismissed(key: String) {
        Log.d(TAG, "Ad dismissed for key: $key, cleaning up")
        
        // Destroy ad để tránh memory leak
        onDestroyAd(key)
        
        // Reset state
        setAdState(key, AdState.IDLE)
        
        // Clear current showing key if it matches
        if (currentShowingKey == key) {
            currentShowingKey = null
        }
    }

    /**
     * Called khi ad show thất bại
     * Các implementation nên gọi method này sau khi show ad thất bại
     * @param key Key của ad đã show thất bại
     */
    protected fun onAdShowFailed(key: String) {
        Log.e(TAG, "Ad show failed for key: $key, setting state to IDLE")
        setAdState(key, AdState.IDLE)
        
        // Clear current showing key if it matches
        if (currentShowingKey == key) {
            currentShowingKey = null
        }
    }

    /**
     * Set state của ad với key
     * @param key Key để identify ad
     * @param state State mới
     */
    private fun setAdState(key: String, state: AdState) {
        adStates[key] = state
        Log.d(TAG, "Ad state changed for key: $key -> $state")
    }

    /**
     * Destroy ad với key cụ thể
     * Các implementation cụ thể sẽ override method này
     * @param key Key của ad cần destroy
     */
    protected abstract fun onDestroyAd(key: String)

    /**
     * Destroy tất cả ads và cleanup resources
     * Các implementation cụ thể sẽ override method này
     */
    override fun destroy() {
        Log.d(TAG, "Destroying all ads")
        
        // Destroy all ads
        adStates.keys.forEach { key ->
            onDestroyAd(key)
        }
        
        // Clear all states and pending shows
        adStates.clear()
        pendingShows.clear()
        currentShowingKey = null
    }

    /**
     * Get current showing key
     * @return Key đang được show, null nếu không có
     */
    fun getCurrentShowingKey(): String? = currentShowingKey
}

