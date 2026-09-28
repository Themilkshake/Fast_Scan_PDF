package com.superfastscan.data.review

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "InAppReviewManager"

@Singleton
class InAppReviewManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    private val reviewManager: ReviewManager by lazy {
        ReviewManagerFactory.create(appContext)
    }

    /**
     * Launches the official Google Play In-App Review flow.
     *
     * If the In-App Review request fails (e.g., in debug builds without Play Store link,
     * missing Play Store app, or API quota limit), it gracefully falls back to opening
     * the app's Google Play Store listing page directly.
     *
     * @param activity Foreground activity required by the In-App Review API.
     * @param onComplete Callback invoked when the review flow or fallback finishes.
     */
    fun launchReviewFlow(activity: Activity, onComplete: () -> Unit = {}) {
        if (activity.isFinishing || activity.isDestroyed) {
            Log.w(TAG, "Activity is finishing or destroyed. Aborting in-app review.")
            onComplete()
            return
        }

        try {
            val request = reviewManager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    if (activity.isFinishing || activity.isDestroyed) {
                        onComplete()
                        return@addOnCompleteListener
                    }

                    val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                    flow.addOnCompleteListener { _ ->
                        Log.d(TAG, "Google Play in-app review flow completed.")
                        onComplete()
                    }
                } else {
                    val exception = task.exception
                    Log.w(TAG, "In-app review request failed: ${exception?.message}. Falling back to Play Store listing.")
                    openPlayStoreListing(activity)
                    onComplete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error launching review flow: ${e.message}", e)
            openPlayStoreListing(activity)
            onComplete()
        }
    }

    /**
     * Opens the app's Google Play Store listing page.
     * Tries market:// URL scheme first, then falls back to https:// web link.
     */
    fun openPlayStoreListing(context: Context) {
        val packageName = context.packageName
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            context.startActivity(marketIntent)
        } catch (e: ActivityNotFoundException) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Could not open Play Store listing: ${e2.message}")
            }
        }
    }
}
