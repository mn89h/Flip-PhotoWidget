package com.pmmn.flipphotowidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.O)

class PhotoWidgetProvider : AppWidgetProvider() {

    override fun onAppWidgetOptionsChanged(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetId: Int, newOptions: Bundle?) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Start a background service or register an alarm
        Log.d("MyWidgetProvider", "Widget added. Starting background service.")
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Iterate over all the active widget instances
        for (appWidgetId in appWidgetIds) {
            // Update each widget
            val views = RemoteViews(context.packageName, R.layout.activity_sub)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    companion object {
        private const val TAG = "SimpleCalendarWidgetProvider"
        val ACTION_ITEM_CLICK = "click"
        val KEY_CALENDAR_ITEM = "data"
        val ACTION_CALENDAR_SYNC = "sync"
        val ACTION_CHANGE_BACKGROUND = "change"
    }

}