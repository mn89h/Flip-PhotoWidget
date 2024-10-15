package com.pmmn.flipphotowidget

import android.R
import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RemoteViews
import com.bumptech.glide.request.target.AppWidgetTarget
import java.util.concurrent.CompletableFuture


class SharedData {
    companion object {
        enum class LayoutDir(val id: Int) {
            VERT(0),
            HORI(1)
        }
        enum class Layouts(val id: Int) {
            ONE(0),
            TWO_1(1),
            TWO_2(2),
            THREE_1(4),
            THREE_2(5),
            THREE_3(6),
            THREE_4(7),
            FOUR(3)
        }


        // Shared data that needs to be persisted
        var layout: Int = 0
        lateinit var tileIds: List<List<Int>>
        var tiles: List<MutableList<LinearLayout>> = listOf(mutableListOf(), mutableListOf())
        lateinit var imageIds: List<List<Int>>
        var images: List<MutableList<ImageView>> = listOf(mutableListOf(), mutableListOf())
        var imageFutures: MutableList<CompletableFuture<Boolean>> = MutableList(4) {
                CompletableFuture.completedFuture(true)}
        var tiles_uris: List<MutableList<Uri>> = List(4) { mutableListOf<Uri>() }
        var uri_iterators: MutableList<Iterator<Uri>> = mutableListOf(
                tiles_uris[0].iterator(), tiles_uris[1].iterator(), tiles_uris[2].iterator(), tiles_uris[3].iterator())

//        // Save the current state of the shared data to SharedPreferences
//        fun saveToPreferences(context: Context) {
//            val sharedPreferences = context.getSharedPreferences("SharedDataPrefs", Context.MODE_PRIVATE)
//            with(sharedPreferences.edit()) {
//                putString("shared_text", sharedText)
//                apply()  // Asynchronous commit
//            }
//        }
//
//        // Load the shared data from SharedPreferences, called on app start or widget update
//        fun loadFromPreferences(context: Context) {
//            val sharedPreferences = context.getSharedPreferences("SharedDataPrefs", Context.MODE_PRIVATE)
//            sharedText = sharedPreferences.getString("shared_text", "Initial Value") ?: "Initial Value"
//        }

//        fun onUpdate(
//            context: Context, layoutId: Int, appWidgetManager: AppWidgetManager,
//            appWidgetIds: IntArray
//        ) {
//            val remoteViews = RemoteViews(context.packageName, layoutId)
//
//            val awt: AppWidgetTarget = object : AppWidgetTarget(context.applicationContext, R.id.img, remoteViews, *appWidgetIds) {
//                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//                    super.onResourceReady(resource, transition)
//                }
//            };
//
//            GlideApp
//                .with(context.applicationContext)
//                .asBitmap()
//                .load(GlideExampleActivity.eatFoodyImages.get(3))
//                .into(awt)
//
//            pushWidgetUpdate(context, remoteViews)
//        }
    }
}