package com.pmmn.flipphotowidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.AppWidgetTarget
import com.bumptech.glide.request.target.Target
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


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

        // Extension function to convert CompletableFuture to a coroutine-friendly suspend function
        suspend fun <T> CompletableFuture<T>.await(): T = suspendCoroutine { cont ->
            this.whenComplete { result, exception ->
                if (exception == null) {
                    cont.resume(result)
                } else {
                    cont.resumeWithException(exception)
                }
            }
        }

        // Shared data that needs to be persisted
        var initialized: Boolean = false
        lateinit var appContext: Context
        lateinit var remoteViews: RemoteViews
        lateinit var appWidgetManager: AppWidgetManager
        lateinit var widgetComponentName: ComponentName
        var layout: Int = 0
        val tileIds: List<List<Int>> = listOf(
            listOf(R.id.idTile1H, R.id.idTile2H, R.id.idTile3H, R.id.idTile4H),
            listOf(R.id.idTile1V, R.id.idTile2V, R.id.idTile3V, R.id.idTile4V))
        val imageIds: List<List<Int>> = listOf(
            listOf(R.id.idImage1H, R.id.idImage2H, R.id.idImage3H, R.id.idImage4H),
            listOf(R.id.idImage1V, R.id.idImage2V, R.id.idImage3V, R.id.idImage4V))
        var tiles_uris: List<MutableList<Uri>> = List(4) { mutableListOf() }

        // Populated here
        var imageFutures: MutableList<CompletableFuture<Boolean>> = MutableList(4) {
                CompletableFuture.completedFuture(true)}
        var uri_iterators: MutableList<ResettableIterator<Uri>> = mutableListOf(
                ResettableIterator(tiles_uris[0]), ResettableIterator(tiles_uris[1]), ResettableIterator(tiles_uris[2]), ResettableIterator(tiles_uris[3]))

        private fun resetImageView(viewId: Int) {
//            val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponentName)
//            appWidgetIds.forEach { appWidgetId ->
//                remoteViews.setImageViewResource(viewId, com.pmmn.flipphotowidget.R.drawable.baseline_add_350)
//            }
        }

        fun loadImageView(imageNo: Int, viewId: Int, indexImage: Int? = null) {
            // Get all widget instances

            // Check if URIs are available
            if (!uri_iterators[imageNo].hasNext()) {
                return
            }

            // Set the current index
            if (indexImage != null) {
                if (!uri_iterators[imageNo].setCurrentIndex(indexImage))
                    return
            }

            // Get the next URI
            val uri: Uri = try {
                uri_iterators[imageNo].next()
            } catch (e: NoSuchElementException) {
                return
            }

            val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponentName)
            appWidgetIds.forEach { appWidgetId ->
                val awt = AppWidgetTarget(appContext, viewId, remoteViews, appWidgetId)

                imageFutures[imageNo] = CompletableFuture()

                Glide
                    .with(appContext)
                    .asBitmap()
                    .load(uri)
                    .listener(object : RequestListener<Bitmap?> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Bitmap?>,
                            isFirstResource: Boolean
                        ): Boolean {
                            Toast.makeText(appContext, "Failed to load image", Toast.LENGTH_SHORT).show()
                            imageFutures[imageNo].complete(true)
                            return false
                        }

                        override fun onResourceReady(
                            resource: Bitmap,
                            model: Any,
                            target: Target<Bitmap?>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            imageFutures[imageNo].complete(true)
                            return false
                        }
                    })
                    .override(600, 600)
                    .into(awt)
            }
        }

        fun updateRemoteLayout() {
            val modLayout = layout % 4
            val modVisibility = layout % 2

            val dir = if (modLayout < 2) LayoutDir.VERT.id else LayoutDir.HORI.id
            val secondTile = layout > Layouts.ONE.id
            val thirdTile = layout >= Layouts.THREE_1.id
            val fourthTile = layout == Layouts.FOUR.id

            val thirdTileNumber = if (modVisibility == 0) 2 else 3

            remoteViews.setViewVisibility(R.id.idContainerVert, View.GONE)
            remoteViews.setViewVisibility(R.id.idContainerHori, View.GONE)

            remoteViews.setViewVisibility(R.id.idTiles13V, View.GONE)
            remoteViews.setViewVisibility(R.id.idTiles13H, View.GONE)
            remoteViews.setViewVisibility(R.id.idTiles24H, View.GONE)
            remoteViews.setViewVisibility(R.id.idTiles24V, View.GONE)

            tileIds.forEach { tileIdDir ->
                tileIdDir.forEach { tileId ->
                    remoteViews.setViewVisibility(tileId, View.GONE)
                }
            }

            if (dir == LayoutDir.VERT.id) {
                remoteViews.setViewVisibility(R.id.idContainerVert, View.VISIBLE)

                remoteViews.setViewVisibility(R.id.idTiles13H, View.VISIBLE)
                if (layout > Layouts.ONE.id) remoteViews.setViewVisibility(R.id.idTiles24H, View.VISIBLE)
            }
            else {
                remoteViews.setViewVisibility(R.id.idContainerHori, View.VISIBLE)

                remoteViews.setViewVisibility(R.id.idTiles13V, View.VISIBLE)
                if (layout > Layouts.ONE.id) remoteViews.setViewVisibility(R.id.idTiles24V, View.VISIBLE)
            }


            remoteViews.setViewVisibility(tileIds[dir][0], View.VISIBLE)
            if (secondTile) {
                remoteViews.setViewVisibility(tileIds[dir][1], View.VISIBLE)
            }
            if (thirdTile) {
                remoteViews.setViewVisibility(tileIds[dir][thirdTileNumber], View.VISIBLE)
            }
            if (fourthTile) {
                remoteViews.setViewVisibility(tileIds[dir][2], View.VISIBLE)
                remoteViews.setViewVisibility(tileIds[dir][3], View.VISIBLE)
            }
        }

        fun updateRemoteImages(imageIndices: List<Int>? = null) {
            val modLayout = layout % 4
            val modVisibility = layout % 2

            val dir = if (modLayout < 2) LayoutDir.VERT.id else LayoutDir.HORI.id
            val secondTile = layout > Layouts.ONE.id
            val thirdTile = layout >= Layouts.THREE_1.id
            val fourthTile = layout == Layouts.FOUR.id

            val thirdTileNumber = if (modVisibility == 0) 2 else 3

            imageIds.forEach { imageIdDir ->
                imageIdDir.forEach { imageId ->
                    resetImageView(imageId)
                }
            }

            loadImageView(0, imageIds[dir][0], imageIndices?.getOrNull(0))
            if (secondTile) {
                loadImageView(1, imageIds[dir][1], imageIndices?.getOrNull(1))
            }
            if (thirdTile) {
                loadImageView(2, imageIds[dir][thirdTileNumber], imageIndices?.getOrNull(2))
            }
            if (fourthTile) {
                loadImageView(2, imageIds[dir][2], imageIndices?.getOrNull(2))
                loadImageView(3, imageIds[dir][3], imageIndices?.getOrNull(3))
            }
        }
    }
}