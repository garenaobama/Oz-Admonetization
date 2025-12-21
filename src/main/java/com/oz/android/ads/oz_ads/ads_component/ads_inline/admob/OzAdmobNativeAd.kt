//package com.oz.android.ads.oz_ads.ads_component.ads_inline.admob
//
//import android.content.Context
//import android.util.AttributeSet
//import android.util.Log
//import com.google.android.gms.ads.nativead.NativeAdView
//import com.oz.android.ads.network.admobs.ads_component.native_advanced.AdmobNativeAdvanced
//import com.oz.android.ads.oz_ads.ads_component.AdsFormat
//import com.oz.android.ads.oz_ads.ads_component.ads_inline.InlineAds
//import java.util.concurrent.ConcurrentHashMap
//
///**
// * Implementation cụ thể của InlineAds cho AdMob Native
// * Chỉ xử lý NATIVE format
// */
//class OzAdmobNativeAd @JvmOverloads constructor(
//    context: Context,
//    attrs: AttributeSet? = null,
//    defStyleAttr: Int = 0
//) : InlineAds<AdmobNativeAdvanced>(context, attrs, defStyleAttr) {
//
//    companion object {
//        private const val TAG = "OzAdmobNativeAd"
//    }
//
//    // Map key -> AdmobNativeAdvanced instance
//    private val nativeAds = ConcurrentHashMap<String, AdmobNativeAdvanced>()
//
//    // Map key -> adUnitId
//    private val adUnitIds = ConcurrentHashMap<String, String>()
//
//    // Map key -> NativeAdView
//    private val nativeAdViews = ConcurrentHashMap<String, NativeAdView>()
//
//    override fun isValidFormat(format: AdsFormat): Boolean {
//        return format == AdsFormat.NATIVE
//    }
//
//    /**
//     * Set ad unit ID cho một key
//     * @param key Key để identify placement
//     * @param adUnitId Ad unit ID từ AdMob
//     */
//    fun setAdUnitId(key: String, adUnitId: String) {
//        adUnitIds[key] = adUnitId
//        Log.d(TAG, "Ad unit ID set for key: $key -> $adUnitId")
//    }
//
//    /**
//     * Set NativeAdView cho một key
//     * @param key Key để identify placement
//     * @param nativeAdView NativeAdView đã được setup
//     */
//    fun setNativeAdView(key: String, nativeAdView: NativeAdView) {
//        nativeAdViews[key] = nativeAdView
//        Log.d(TAG, "NativeAdView set for key: $key")
//    }
//
//    /**
//     * Get ad unit ID cho một key
//     * @param key Key để identify placement
//     * @return Ad unit ID, null nếu chưa được set
//     */
//    fun getAdUnitId(key: String): String? = adUnitIds[key]
//
//    override fun createAd(key: String): AdmobNativeAdvanced? {
//        val adUnitId = adUnitIds[key]
//        if (adUnitId == null) {
//            Log.e(TAG, "Ad unit ID not set for key: $key")
//            return null
//        }
//        return AdmobNativeAdvanced(context, adUnitId)
//    }
//
//    override fun onLoadAd(key: String, ad: AdmobNativeAdvanced) {
//        ad.setAdmobNativeAdListener(object : AdmobNativeAdvanced.AdmobNativeAdListener {
//            override fun onAdLoaded() {
//                this@OzAdmobNativeAd.onAdLoaded(key, ad)
//            }
//
//            override fun onAdFailedToLoad(error: String) {
//                this@OzAdmobNativeAd.onAdLoadFailed(key, error)
//            }
//
//            override fun onAdOpened() {
//                // Not relevant for native ads in this context
//            }
//
//            override fun onAdClosed() {
//                // Not relevant for native ads in this context
//            }
//
//            override fun onAdClicked() {
//                // Not relevant for native ads in this context
//            }
//        })
//        ad.load()
//    }
//
//    override fun onShowAds(key: String, ad: AdmobNativeAdvanced) {
//        val nativeAdView = nativeAdViews[key]
//        if (nativeAdView == null) {
//            Log.e(TAG, "NativeAdView not set for key: $key")
//            onAdShowFailed(key, "NativeAdView not set")
//            return
//        }
//        ad.show(this, nativeAdView)
//        onAdShown(key)
//    }
//
//    override fun onDestroyAd(ad: AdmobNativeAdvanced) {
//        ad.destroy()
//    }
//
//    override fun onPauseAd() {
//        // Native ads generally don't need explicit pause handling
//    }
//
//    override fun onResumeAd() {
//        // Native ads generally don't need explicit resume handling
//    }
//}
//}
//
