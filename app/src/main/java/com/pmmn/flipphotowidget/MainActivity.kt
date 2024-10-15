
package com.pmmn.flipphotowidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RemoteViews
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ReportFragment.Companion.reportFragment
import com.pmmn.flipphotowidget.SharedData.Companion.layout
import com.pmmn.flipphotowidget.SharedData.Companion.imageIds
import com.pmmn.flipphotowidget.SharedData.Companion.images
import com.pmmn.flipphotowidget.SharedData.Companion.tileIds
import com.pmmn.flipphotowidget.SharedData.Companion.tiles
import com.pmmn.flipphotowidget.SharedData.Companion.tiles_uris
import com.pmmn.flipphotowidget.SharedData.Companion.LayoutDir
import com.pmmn.flipphotowidget.SharedData.Companion.Layouts
import java.io.IOException

class MainActivity : AppCompatActivity() {

    // creating variables on below line.
    private lateinit var btnAddTile: Button
    lateinit var btnRemTile: Button
    lateinit var btnCycle: Button

    private lateinit var remoteViews: RemoteViews
    private lateinit var previewView: View
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var widgetComponentName: ComponentName


    private var selected_tile = 1

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


                try {
                    // Load the image from the Uri as a Bitmap
                    val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uris.first())

                    // Set the image using setImageViewBitmap
                    imageIds.forEach { imageIdDir ->
//                        remoteViews.setImageViewUri(imageIdDir[selected_tile], uris.first())
                        remoteViews.setImageViewBitmap(imageIdDir[selected_tile], bitmap)
                    }

                    updateViews()

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                Log.d("PhotoPicker", "No media selected")
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

        remoteViews.setViewVisibility(tileIds[dir][0], View.VISIBLE)
        if (secondTile) remoteViews.setViewVisibility(tileIds[dir][1], View.VISIBLE)
        if (thirdTile) remoteViews.setViewVisibility(tileIds[dir][thirdTileNumber], View.VISIBLE)
        if (fourthTile) {
            remoteViews.setViewVisibility(tileIds[dir][2], View.VISIBLE)
            remoteViews.setViewVisibility(tileIds[dir][3], View.VISIBLE)
        }

        updateViews()
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
