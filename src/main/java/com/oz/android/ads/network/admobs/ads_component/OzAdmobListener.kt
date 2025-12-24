package com.oz.android.ads.network.admobs.ads_component

import AdmobBanner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError

abstract class OzAdmobListener<AdType> {
    open fun onAdLoaded(ad: AdType) {}
    open fun onAdFailedToLoad(error: LoadAdError) {}
    open fun onAdClicked() {}
    open fun onAdImpression() {}
    
    // Fullscreen content callbacks (for Interstitial, Rewarded, App Open ads)
    open fun onAdShowedFullScreenContent() {}
    open fun onAdDismissedFullScreenContent() {}
    open fun onAdFailedToShowFullScreenContent(adError: AdError) {}
}