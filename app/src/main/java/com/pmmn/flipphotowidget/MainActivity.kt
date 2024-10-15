
package com.pmmn.flipphotowidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.AppWidgetTarget
import com.bumptech.glide.request.transition.Transition
import com.pmmn.flipphotowidget.SharedData.Companion.layout
import com.pmmn.flipphotowidget.SharedData.Companion.imageIds
import com.pmmn.flipphotowidget.SharedData.Companion.images
import com.pmmn.flipphotowidget.SharedData.Companion.tileIds
import com.pmmn.flipphotowidget.SharedData.Companion.tiles
import com.pmmn.flipphotowidget.SharedData.Companion.tiles_uris
import com.pmmn.flipphotowidget.SharedData.Companion.LayoutDir
import com.pmmn.flipphotowidget.SharedData.Companion.Layouts
import com.pmmn.flipphotowidget.SharedData.Companion.imageFutures
import com.pmmn.flipphotowidget.SharedData.Companion.uri_iterators
import kotlinx.coroutines.*
import java.io.IOException
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity() {

    // creating variables on below line.
    private lateinit var btnAddTile: Button
    lateinit var btnRemTile: Button
    lateinit var btnCycle: Button
    var imageLoaded: Boolean = false

    private lateinit var remoteViews: RemoteViews
    private lateinit var previewView: View
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var widgetComponentName: ComponentName


    private var selected_tile = 1

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

    private fun  updateViews() {
        for (appWidgetId in appWidgetManager.getAppWidgetIds(widgetComponentName)) {
            // Update the widget using AppWidgetManager
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)


            // Inflate the RemoteViews into a standard View
            val remoteViewContainer = findViewById<LinearLayout>(R.id.idPreviewContainer)
            previewView = remoteViews.apply(applicationContext, remoteViewContainer)

            // Add the inflated RemoteViews to the container
            remoteViewContainer.removeAllViews()
            remoteViewContainer.addView(previewView)


            imageIds.forEachIndexed { i, imageIdDir ->
                images[i].clear()
                imageIdDir.forEach { imageId ->
                    images[i].add(findViewById(imageId))
                }
            }

            // adding click listener for button on below line.
            images.forEach { imageDir ->
                imageDir.forEachIndexed { i, image ->
                    image.setOnClickListener {
                        selected_tile = i
                        pickMultipleMedia.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                }
            }
        }
    }

    // Registers a photo picker activity launcher in multi-select mode.
    private val pickMultipleMedia: ActivityResultLauncher<PickVisualMediaRequest> =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            // Callback is invoked after the user selects media items or closes the
            // photo picker.
            if (uris.isNotEmpty()) {
                Log.d("PhotoPicker", "Number of items selected: ${uris.size}")
                tiles_uris[selected_tile].clear()
                tiles_uris[selected_tile].addAll(uris)

                updateTileVisibilityAndLayout()
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }

    private fun resetImageView(viewId: Int) {
        val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponentName)
        appWidgetIds.forEach { appWidgetId ->
            remoteViews.setImageViewResource(viewId, R.drawable.baseline_add_350)
        }
    }

    private fun loadImageView(imageNo: Int, viewId: Int) {
        val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponentName)
        appWidgetIds.forEach { appWidgetId ->
            val awt: AppWidgetTarget = object : AppWidgetTarget(applicationContext, viewId, remoteViews, appWidgetId) {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    super.onResourceReady(resource, transition)
                }
            }

            if (tiles_uris[imageNo].isNotEmpty()) {
                imageFutures[imageNo] = CompletableFuture()
                var uri: Uri
                if (uri_iterators[imageNo].hasNext()) {
                    uri = uri_iterators[imageNo].next()
                } else {
                    uri_iterators[imageNo] = tiles_uris[imageNo].iterator()
                    uri = uri_iterators[imageNo].next()
                }

                Glide
                    .with(applicationContext)
                    .asBitmap()
                    .load(uri)
                    .listener(object : RequestListener<Bitmap?> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Bitmap?>,
                            isFirstResource: Boolean
                        ): Boolean {
                            Toast.makeText(applicationContext, "Failed to load image", Toast.LENGTH_SHORT).show()
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
                    .override(300, 300)
                    .into(awt)

            }
        }
    }

    private fun updateTileVisibilityAndLayout() {
        val modLayout = layout % 4
        val modVisibility = layout % 2

        val dir = if (modLayout < 2) LayoutDir.VERT.id else LayoutDir.HORI.id
        val secondTile = if (layout > Layouts.ONE.id) true else false
        val thirdTile = if (layout >= Layouts.THREE_1.id) true else false
        val fourthTile = if (layout == Layouts.FOUR.id) true else false

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

        imageIds.forEach { imageIdDir ->
            imageIdDir.forEach { imageId ->
                resetImageView(imageId)
            }
        }

        remoteViews.setViewVisibility(tileIds[dir][0], View.VISIBLE)
        loadImageView(0, imageIds[dir][0])
        if (secondTile) {
            remoteViews.setViewVisibility(tileIds[dir][1], View.VISIBLE)
            loadImageView(1, imageIds[dir][1])
        }
        if (thirdTile) {
            remoteViews.setViewVisibility(tileIds[dir][thirdTileNumber], View.VISIBLE)
            loadImageView(2, imageIds[dir][thirdTileNumber])
        }
        if (fourthTile) {
            remoteViews.setViewVisibility(tileIds[dir][2], View.VISIBLE)
            remoteViews.setViewVisibility(tileIds[dir][3], View.VISIBLE)
            loadImageView(2, imageIds[dir][2])
            loadImageView(3, imageIds[dir][3])
        }

        // Needed coroutine in a non-blocking way, allowing Glide to run on the main thread
        CoroutineScope(Dispatchers.Main).launch {
            try {
                imageFutures.forEach { future ->
                    future.await()  // Non-blocking wait for the future to complete
                }
                updateViews()
            } catch (e: Exception) {
                println("Failed: ${e.message}")
            }
        }
    }

    private fun cycleValue(value: Int, maxValue: Int, minValue: Int = 0, increment: Boolean = true): Int {
        val updatedValue = if(increment) value+1 else value-1

        return when {
            updatedValue > maxValue -> minValue
            updatedValue < minValue -> maxValue
            else -> updatedValue
        }
    }

    private fun resetLayout(layout: Int, oldTileCount: Int, newTileCount: Int): Int {
        if (oldTileCount == newTileCount)
            return layout

        return when (newTileCount) {
            1 -> 0
            2 -> 1
            3 -> 4 // up to 7
            4 -> 3
            else -> 0
        }
    }

    private fun cycleLayout(layout: Int, tileCount: Int): Int {
        return when (tileCount) {
            1 -> 0
            2 -> cycleValue(layout, 2, 1)
            3 -> cycleValue(layout, 7, 4)
            4 -> 3
            else -> 0
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find the container where RemoteViews will be displayed
        val remoteViewContainer = findViewById<LinearLayout>(R.id.idPreviewContainer)

        // Create a RemoteViews object pointing to the layout
        remoteViews = RemoteViews(packageName, R.layout.activity_sub)

        // Inflate the RemoteViews into a standard View
        previewView = remoteViews.apply(this, remoteViewContainer)

        // Add the inflated RemoteViews to the container
        remoteViewContainer.addView(previewView)

        appWidgetManager = AppWidgetManager.getInstance(this)
        widgetComponentName = ComponentName(getApplicationContext(), PhotoWidgetProvider::class.java)

        tileIds  = listOf(
            listOf(R.id.idTile1H, R.id.idTile2H, R.id.idTile3H, R.id.idTile4H),
            listOf(R.id.idTile1V, R.id.idTile2V, R.id.idTile3V, R.id.idTile4V))
        tileIds.forEachIndexed { i, tileIdDir ->
            tileIdDir.forEach { tileId ->
                tiles[i].add(findViewById(tileId))
            }
        }
        imageIds = listOf(
            listOf(R.id.idImage1H, R.id.idImage2H, R.id.idImage3H, R.id.idImage4H),
            listOf(R.id.idImage1V, R.id.idImage2V, R.id.idImage3V, R.id.idImage4V))
        imageIds.forEachIndexed { i, imageIdDir ->
            images[i].clear()
            imageIdDir.forEach { imageId ->
                images[i].add(findViewById(imageId))
            }
        }

        btnAddTile = findViewById(R.id.idBtnAddTile)
        btnAddTile.setOnClickListener {
            layout = cycleValue(layout, 7, 0, true)
            updateTileVisibilityAndLayout()
        }

        // adding click listener for button on below line.
        images.forEach { imageDir ->
            imageDir.forEachIndexed { i, image ->
                image.setOnClickListener {
                    selected_tile = i
                    pickMultipleMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            }
        }


//        btnAddTile = findViewById(R.id.idBtnAddTile)
//        btnRemTile = findViewById(R.id.idBtnRemTile)
//        btnCycle = findViewById(R.id.idBtnCycle)
//
//        btnAddTile.setOnClickListener {
//            val tileCtrOld = tileCtr
//            tileCtr = cycleValue(tileCtr, 4, 1, true)
//            layout = resetLayout(layout, tileCtrOld, tileCtr)
//            updateTileVisibilityAndLayout()
//            updateWidgetCall()
//        }
//
//        btnRemTile.setOnClickListener {
//            val tileCtrOld = tileCtr
//            tileCtr = cycleValue(tileCtr, 4, 1, false)
//            layout = resetLayout(layout, tileCtrOld, tileCtr)
//            updateTileVisibilityAndLayout()
//            updateWidgetCall()
//        }
//
//        btnCycle.setOnClickListener {
//            layout = cycleLayout(layout, tileCtr)
//            updateTileVisibilityAndLayout()
//            updateWidgetCall()
//        }


    }

    private fun updateWidgetVisibility() {

        remoteViews.setViewVisibility(R.id.idTile1H, View.GONE)
//      remoteViews.setImageViewResource(R.id.idTile1, R.drawable.baseline_close_24)
        for (appWidgetId in appWidgetManager.getAppWidgetIds(widgetComponentName)) {
            // Update the widget using AppWidgetManager
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
            remoteViews.reapply(this, previewView)
        }

    }

}
