# OzAds Architecture

## Overview

The OzAds library provides a robust, scalable, and easy-to-use framework for integrating various ad networks into an Android application. It is designed with a clear separation of concerns, allowing developers to easily add new ad formats or networks with minimal effort.

The architecture is divided into three main layers:
1.  **Business Logic Layer (`/oz_ads/ads_component`)**: Manages ad state, lifecycle, and presentation logic.
2.  **Concrete Implementation Layer (`/oz_ads/ads_component/ads_inline/admob`)**: Connects a specific ad network (e.g., AdMob) to the business logic layer.
3.  **Network Layer (`/network`)**: Provides a thin wrapper around the ad network's SDK.

---

## Core Components

### 1. `OzAds<AdType>` (Abstract Base Class)

This is the cornerstone of the ad business logic. It's a generic abstract class responsible for:

*   **State Management**: Tracks the state of each ad (`IDLE`, `LOADING`, `LOADED`, `SHOWING`).
*   **Ad Object Storage**: Securely stores the loaded ad objects of a generic type (`AdType`) in a map, where the key represents the ad placement.
*   **Orchestration**: Defines the core workflow for loading and showing ads through abstract methods (`createAd`, `onLoadAd`, `onShowAds`).

This generic design allows the framework to handle any type of ad object without being tied to a specific ad network's implementation.

### 2. `InlineAds<AdType>` (Abstract `ViewGroup`)

`InlineAds` is a specialized `ViewGroup` designed to display inline ads such as banners and native ads. It extends `ViewGroup` and contains an instance of `OzAds<AdType>` to manage the underlying logic.

Key responsibilities include:

*   **UI Container**: Acts as a container in the layout for the ad view.
*   **Lifecycle Awareness**: Automatically handles ad pausing, resuming, and destruction by observing Android's view lifecycle (`onAttachedToWindow`, `onDetachedFromWindow`, `onVisibilityChanged`).
*   **Auto-Refresh**: Provides a mechanism to automatically refresh ads at a configurable interval.

### 3. Concrete Implementations (e.g., `OzAdmobBannerAd`)

These classes provide the concrete implementation for a specific ad format and network. For example, `OzAdmobBannerAd` is an implementation for AdMob banner ads.

Its role is greatly simplified in the new architecture:

*   It extends `InlineAds<AdmobBanner>`.
*   It implements the abstract methods to:
    *   **Create**: Instantiate the network-specific ad object (e.g., `AdmobBanner`).
    *   **Load**: Trigger the ad load on the network object.
    *   **Show**: Display the ad view within the `InlineAds` container.
    *   **Destroy**: Clean up the network object.

```kotlin
// Example: OzAdmobBannerAd is now simple and focused
class OzAdmobBannerAd() : InlineAds<AdmobBanner>() {

    override fun createAd(key: String): AdmobBanner? {
        // ... create AdmobBanner with a listener
    }

    override fun onLoadAd(key: String, ad: AdmobBanner) {
        ad.load()
    }

    override fun onShowAds(key: String, ad: AdmobBanner) {
        ad.show(this) // 'this' is the ViewGroup
    }

    override fun destroyAd(ad: AdmobBanner) {
        ad.destroy()
    }
    
    // ...
}
```

### 4. Network Layer (e.g., `AdmobBanner`)

Classes in the `/network` directory are direct wrappers around the ad network SDKs. For example, `AdmobBanner` wraps Google's `AdView`.

*   **Responsibilities**: Manages the low-level details of requesting, receiving, and displaying an ad from a specific SDK.
*   **Callbacks**: Uses listener interfaces (e.g., `AdmobBannerListener`) to communicate events like `onAdLoaded` or `onAdFailedToLoad` back up to the business logic layer.

---

## How to Use

1.  **Add to Layout**: Place the specific ad view (`OzAdmobBannerAd`) in your XML layout.

    ```xml
    <com.oz.android.ads.oz_ads.ads_component.ads_inline.admob.OzAdmobBannerAd
        android:id="@+id/my_banner_ad"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
    ```

2.  **Configure in Code**: In your Activity or Fragment, get a reference to the view and configure it with an ad unit ID and a placement key.

    ```kotlin
    val bannerAd = findViewById<OzAdmobBannerAd>(R.id.my_banner_ad)
    
    // Set the Ad Unit ID for a specific placement
    bannerAd.setAdUnitId("my_placement_key", "ca-app-pub-...")
    
    // Preload the ad
    bannerAd.setPreloadKey("my_placement_key")
    
    // The ad will automatically load and show based on view lifecycle
    ```

3.  **Lifecycle Management**: The `InlineAds` view automatically handles `pause`, `resume`, and `destroy` events. No manual calls are needed in the Activity/Fragment lifecycle methods.
