package com.focusguard.app.ad

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdRewardManager {
    private const val TAG = "AdRewardManager"

    // Google Official Rewarded Video Test Ad Unit ID
    private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private var rewardedAd: RewardedAd? = null
    private var isLoading: Boolean = false

    fun isAdLoaded(): Boolean = rewardedAd != null

    fun preloadAd(context: Context) {
        if (rewardedAd != null || isLoading) {
            return
        }

        isLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context.applicationContext,
            TEST_REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Failed to load rewarded ad: ${loadAdError.message}")
                    rewardedAd = null
                    isLoading = false
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.i(TAG, "Rewarded ad successfully preloaded and ready.")
                    rewardedAd = ad
                    isLoading = false
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onAdDismissedWithoutReward: () -> Unit,
        onAdFailedToShow: (reason: String) -> Unit
    ) {
        val currentAd = rewardedAd
        if (currentAd == null) {
            preloadAd(activity)
            onAdFailedToShow("광고를 불러오는 중이거나 네트워크가 연결되지 않았습니다.")
            return
        }

        var rewardEarned = false

        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                Log.d(TAG, "Rewarded ad clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed. rewardEarned=$rewardEarned")
                rewardedAd = null
                // Preload the next ad in advance
                preloadAd(activity)

                if (!rewardEarned) {
                    onAdDismissedWithoutReward()
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Failed to show rewarded ad: ${adError.message}")
                rewardedAd = null
                preloadAd(activity)
                onAdFailedToShow(adError.message)
            }

            override fun onAdImpression() {
                Log.d(TAG, "Rewarded ad recorded impression.")
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed fullscreen content.")
            }
        }

        currentAd.show(activity) { rewardItem ->
            Log.i(TAG, "User completed watching rewarded ad! Earned: ${rewardItem.amount} ${rewardItem.type}")
            rewardEarned = true
            onUserEarnedReward()
        }
    }
}
