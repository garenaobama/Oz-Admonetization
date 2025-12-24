package com.oz.android.ads.network.admobs.ads_component

import android.content.Context

/**
 * @param AdType The type of the Ad Object (AdView, InterstitialAd, NativeAd, etc.)
 */
abstract class AdmobBase<AdType>(
    val context: Context,
    var adUnitId: String,
    var listener: OzAdmobListener<AdType>? = null // Default to null = Optional
) {

    /**
     * Standard Load method
     */
    abstract fun load()

    /**
     * Abstract Show method.
     * Note: Subclasses can overload this (e.g., show(container)).
     */
    abstract fun show()

    /**
     * Common logic for "Load then immediately Show".
     * This relies on the listener being set.
     */
    abstract fun loadThenShow()
}