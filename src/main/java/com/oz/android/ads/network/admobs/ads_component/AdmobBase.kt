package com.oz.android.ads.network.admobs.ads_component

import android.content.Context

open class AdmobBase<AdType>(
    val context: Context, var adUnitId: String, val listener: OzAdmobListener<AdType>?
)