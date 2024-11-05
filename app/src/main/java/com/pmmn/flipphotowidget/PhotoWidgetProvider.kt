package com.pmmn.flipphotowidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import com.pmmn.flipphotowidget.SharedData.Companion
import com.pmmn.flipphotowidget.SharedData.Companion.appContext
import com.pmmn.flipphotowidget.SharedData.Companion.appWidgetManager
import com.pmmn.flipphotowidget.SharedData.Companion.imageIds
import com.pmmn.flipphotowidget.SharedData.Companion.initialized
import com.pmmn.flipphotowidget.SharedData.Companion.loadImageView
import com.pmmn.flipphotowidget.SharedData.Companion.remoteViews
import com.pmmn.flipphotowidget.SharedData.Companion.tiles_uris
import com.pmmn.flipphotowidget.SharedData.Companion.updateRemoteImages
import com.pmmn.flipphotowidget.SharedData.Companion.updateRemoteLayout
import com.pmmn.flipphotowidget.SharedData.Companion.uri_iterators
import com.pmmn.flipphotowidget.SharedData.Companion.widgetComponentName

class PhotoWidgetProvider : AppWidgetProvider() {

    private fun setClickListeners(context: Context) {
        Log.d("MyWidgetProvider", "Setting click listeners.")
        if(tiles_uris.isEmpty()) initialize(context)
        appContext = context
        remoteViews = RemoteViews(context.packageName, R.layout.activity_sub)
        appWidgetManager = AppWidgetManager.getInstance(context)
        widgetComponentName = ComponentName(context, PhotoWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponentName)

        // Iterate over all the active widget instances
        appWidgetIds.forEach { wid ->

            // Create PendingIntent for every image
            val pendingIntents: List<List<PendingIntent>> = List(4) { i ->
                List(4) { j ->
                    Intent(context, PhotoWidgetProvider::class.java).run {
                        // Assuming 'it' is the app widget ID in this context
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid)
                        putExtra("IMAGEIDDIR", i)
                        putExtra("IMAGEID", j)
                        action = "IMAGE_CLICK"

                        // Create a PendingIntent with incremented request code (id)
                        PendingIntent.getBroadcast(
                            context,
                            i * 4 + j, // Use incremented 'id' as the unique request code
                            this,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    }
                }
            }

            // Attach the PendingIntents to the views in the widget
            imageIds.forEachIndexed { i, imageIdDir ->
                imageIdDir.forEachIndexed { j, imageId ->
                    remoteViews.setOnClickPendingIntent(imageId, pendingIntents[i][j])
                }
            }
            // Update the widget with the RemoteViews
            appWidgetManager.updateAppWidget(wid, Companion.remoteViews)
        }
    }

    fun initialize(context: Context) {
        Log.d("MyWidgetProvider", "Initializing.")
        // Create a RemoteViews object pointing to the layout
        appContext = context
        remoteViews = RemoteViews(context.packageName, R.layout.activity_sub)
        appWidgetManager = AppWidgetManager.getInstance(context)
        widgetComponentName = ComponentName(context, PhotoWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponentName)


        val sharedPreferences = context.getSharedPreferences("Config", Context.MODE_PRIVATE)

        SharedData.layout = sharedPreferences.getInt("layout", 0)
        tiles_uris.forEachIndexed { i, uris_i ->
            // Retrieve the Set<String> from SharedPreferences and convert it to List<Uri>
            val uriStrings = sharedPreferences.getStringSet("uri_set_$i", emptySet())!!.toList()

            // Clear the existing list and populate with the new URIs
            uris_i.clear()
            uris_i.addAll(uriStrings.map { Uri.parse(it) })
            uri_iterators[i] = ResettableIterator(uris_i)
        }
        val imageIndicesList: List<Int>? = sharedPreferences.getStringSet("indices_set", emptySet())
            ?.map { it.toInt() }
        updateRemoteLayout()
        updateRemoteImages(imageIndicesList)


        // Iterate over all the active widget instances
        appWidgetIds.forEach { wid ->
            // Update the widget with the RemoteViews
            appWidgetManager.updateAppWidget(wid, remoteViews)
        }

        initialized = true
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        context.startActivity(intent)
    }

    override fun onEnabled(context: Context) {
        Log.d("MyWidgetProvider", "Widget enabled.")
        if(!initialized) initialize(context)
        super.onEnabled(context)
        // Start a background service or register an alarm
        Log.d("MyWidgetProvider", "Widget added. Starting background service.")
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("MyWidgetProvider", "Widget updated.")
        if(!initialized) initialize(context)
        setClickListeners(context)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("MyWidgetProvider", "Widget received intent.")
        if(!initialized) initialize(context!!)
        setClickListeners(context!!)

        // Handle the widget button click event
        if (intent?.action == "IMAGE_CLICK") {
            val i = intent.getIntExtra("IMAGEIDDIR", 0)
            val j = intent.getIntExtra("IMAGEID", 0)

            loadImageView(j, imageIds[i][j])

            // Save image indices in SharedPreferences
            val sharedPreferences = context.getSharedPreferences("Config", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            // Loop through each MutableList<Uri> and store it as a Set<String>
            val indices_set = uri_iterators.map { it.getCurrentIndex().toString() }.toSet()
            editor.putStringSet("indices_set", indices_set)
            editor.apply()

        }

        appWidgetManager.updateAppWidget(widgetComponentName, remoteViews)
    }

}