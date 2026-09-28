package com.superfastscan.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

const val BANNER_HOME_AD_UNIT_ID = "ca-app-pub-3810552981107741/5092248373"

const val BANNER_SAVED_SCANS_AD_UNIT_ID = "ca-app-pub-3810552981107741/9070251327"

@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = BANNER_HOME_AD_UNIT_ID,
    isPremium: Boolean = false
) {
    if (isPremium) {
        Spacer(modifier = Modifier.size(0.dp))
        return
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
