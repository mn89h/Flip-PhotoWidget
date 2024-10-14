package com.pmmn.flipphotowidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.O)

class RemoteViewService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent?): RemoteViewsFactory {
        return PhotoWidgetRemoteViewsFactory(this, intent)
    }

    class PhotoWidgetRemoteViewsFactory(
        private val context: Context,
        private val intent: Intent?
    ) : RemoteViewsFactory {

        private val appWidgetId: Int = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        override fun onCreate() {
        }

        override fun onDataSetChanged() {
        }

        override fun onDestroy() {
        }

        override fun getCount(): Int {
            TODO("Not yet implemented")
            return 1
        }

        override fun getViewAt(position: Int): RemoteViews {
            return RemoteViews(context.packageName, R.layout.activity_sub)
        }

        override fun getLoadingView(): RemoteViews =
            RemoteViews(context.packageName, R.layout.activity_sub)

        override fun getViewTypeCount(): Int = 2

        override fun getItemId(position: Int): Long = position.toLong()

        override fun hasStableIds(): Boolean = false


        companion object {
            private const val TAG = "SampleWidgetRemoteViewService"
            private const val PERIOD_TO_SHOW = 7
        }
    }
}