
package com.pmmn.flipphotowidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RemoteViews
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // creating variables on below line.
    lateinit var containerTop: LinearLayout
    lateinit var container1: LinearLayout
    lateinit var container2: LinearLayout
    lateinit var btnAddTile: Button
    lateinit var btnRemTile: Button
    lateinit var btnCycle: Button
    var tiles: MutableList<ImageView> = mutableListOf<ImageView>()

    private var tileCtr = 1
    private var layout = 0

    private var selected_tile = 1
    private var tiles_uris: List<MutableList<Uri>> = List(4) { mutableListOf<Uri>() }


    // Registers a photo picker activity launcher in multi-select mode.
    private val pickMultipleMedia: ActivityResultLauncher<PickVisualMediaRequest> =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            // Callback is invoked after the user selects media items or closes the
            // photo picker.
            if (uris.isNotEmpty()) {
                Log.d("PhotoPicker", "Number of items selected: ${uris.size}")
                tiles_uris[selected_tile].clear()
                tiles_uris[selected_tile].addAll(uris)
                tiles[selected_tile].setImageURI(uris.first())
                tiles[selected_tile].scaleType = ImageView.ScaleType.CENTER_CROP
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }

    private fun updateTileVisibilityAndLayout() {
        val modLayout = layout % 4
        val modVisibility = layout % 2

        if (modLayout < 2) {
            containerTop.orientation = LinearLayout.VERTICAL
            container1.orientation = LinearLayout.HORIZONTAL
            container2.orientation = LinearLayout.HORIZONTAL
        }
        else {
            containerTop.orientation = LinearLayout.HORIZONTAL
            container1.orientation = LinearLayout.VERTICAL
            container2.orientation = LinearLayout.VERTICAL
        }

        container1.visibility = View.VISIBLE
        if (layout == 0)
            container2.visibility = View.GONE
        else
            container2.visibility = View.VISIBLE

        tiles[0].visibility = View.VISIBLE
        tiles[1].visibility = View.VISIBLE

        if (layout == 3) {
            tiles[2].visibility = View.VISIBLE
            tiles[3].visibility = View.VISIBLE
        }
        else if (layout < 3) {
            tiles[2].visibility = View.GONE
            tiles[3].visibility = View.GONE
        }
        else {
            if (modVisibility == 0) {
                tiles[2].visibility = View.VISIBLE
                tiles[3].visibility = View.GONE
            }
            else {
                tiles[2].visibility = View.GONE
                tiles[3].visibility = View.VISIBLE
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

        // initializing variables on below line.
        containerTop = findViewById(R.id.idContainerTop)
        container1 = findViewById(R.id.idContainer1)
        container2 = findViewById(R.id.idContainer2)
        btnAddTile = findViewById(R.id.idBtnAddTile)
        btnRemTile = findViewById(R.id.idBtnRemTile)
        btnCycle = findViewById(R.id.idBtnCycle)
        tiles.add(findViewById(R.id.idTile1))
        tiles.add(findViewById(R.id.idTile2))
        tiles.add(findViewById(R.id.idTile3))
        tiles.add(findViewById(R.id.idTile4))

        btnAddTile.setOnClickListener {
            val tileCtrOld = tileCtr
            tileCtr = cycleValue(tileCtr, 4, 1, true)
            layout = resetLayout(layout, tileCtrOld, tileCtr)
            updateTileVisibilityAndLayout()
            updateWidgetCall()
        }

        btnRemTile.setOnClickListener {
            val tileCtrOld = tileCtr
            tileCtr = cycleValue(tileCtr, 4, 1, false)
            layout = resetLayout(layout, tileCtrOld, tileCtr)
            updateTileVisibilityAndLayout()
            updateWidgetCall()
        }

        btnCycle.setOnClickListener {
            layout = cycleLayout(layout, tileCtr)
            updateTileVisibilityAndLayout()
            updateWidgetCall()
        }

        // adding click listener for button on below line.
        tiles.forEachIndexed { i, tile ->
            tile.setOnClickListener {
                selected_tile = i
                pickMultipleMedia.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
                updateWidgetCall()
            }
        }

    }

    private fun updateWidgetCall() {
        val context = this
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // Get the component name for your AppWidgetProvider
        val componentName = ComponentName(context, PhotoWidgetProvider::class.java)

        // Get all app widget IDs associated with this provider
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        // Update the widget layout via RemoteViews
        for (appWidgetId in appWidgetIds) {
            val remoteViews = RemoteViews(packageName, R.layout.activity_sub)

            // remoteViews.setImageViewResource(R.id.idTile1, R.drawable.baseline_close_24)

            // Update the widget using AppWidgetManager
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }

    }

}
