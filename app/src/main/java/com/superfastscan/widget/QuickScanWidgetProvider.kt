package com.superfastscan.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import com.superfastscan.MainActivity
import com.superfastscan.R
import com.superfastscan.data.local.datastore.SettingsDataStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * AppWidgetProvider for Fast Scan PDF.
 * Places a quick scan shortcut widget on the user's home screen.
 * Dynamically displays ic_app_logo (Free) or ic_app_logo_premium (Premium) based on entitlement.
 */
class QuickScanWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun settingsDataStore(): SettingsDataStore
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val isPremium = runCatching {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java
            )
            runBlocking(Dispatchers.IO) {
                entryPoint.settingsDataStore().isPremiumUser.first()
            }
        }.getOrDefault(false)

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, isPremium)
        }
    }

    companion object {
        const val ACTION_START_SCAN = "com.superfastscan.action.START_SCAN"
        const val EXTRA_START_SCAN = "extra_start_scan"
        private const val TAG = "QuickScanWidgetProvider"

        /**
         * Updates a single widget with appropriate branding and pending scan intent.
         */
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            isPremium: Boolean
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_scan)

            // Dynamic 1-block logo: widget_logo_standard (Free) vs widget_logo_premium (Premium)
            val logoRes = if (isPremium) R.drawable.widget_logo_premium else R.drawable.widget_logo_standard
            views.setImageViewResource(R.id.widget_logo, logoRes)

            // Intent to launch MainActivity with instant scan trigger
            val scanIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_START_SCAN
                putExtra(EXTRA_START_SCAN, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                scanIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_logo, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /**
         * Broadcasts an update to all active widget instances (e.g., when Premium state changes).
         */
        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, QuickScanWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds.isNotEmpty()) {
                    val intent = Intent(context, QuickScanWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    }
                    context.sendBroadcast(intent)
                    Log.d(TAG, "Sent update broadcast to ${appWidgetIds.size} widgets")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update widgets", e)
            }
        }

        /**
         * Checks if the device launcher supports programmatic widget pinning.
         */
        fun isPinSupported(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
            } else {
                false
            }
        }

        /**
         * Prompts the user to pin the quick scan widget to their home screen.
         * Passes the correct tier logo in the preview bundle.
         */
        fun requestPinWidget(context: Context, isPremium: Boolean): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    val provider = ComponentName(context, QuickScanWidgetProvider::class.java)
                    val previewViews = RemoteViews(context.packageName, R.layout.widget_quick_scan).apply {
                        val logoRes = if (isPremium) R.drawable.widget_logo_premium else R.drawable.widget_logo_standard
                        setImageViewResource(R.id.widget_logo, logoRes)
                    }
                    val bundle = Bundle().apply {
                        putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, previewViews)
                    }
                    return appWidgetManager.requestPinAppWidget(provider, bundle, null)
                }
            }
            return false
        }
    }
}
