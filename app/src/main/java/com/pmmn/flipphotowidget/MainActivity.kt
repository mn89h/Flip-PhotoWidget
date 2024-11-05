
package com.pmmn.flipphotowidget

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.pmmn.flipphotowidget.SharedData.Companion.layout
import com.pmmn.flipphotowidget.SharedData.Companion.imageIds
import com.pmmn.flipphotowidget.SharedData.Companion.tileIds
import com.pmmn.flipphotowidget.SharedData.Companion.tiles_uris
import com.pmmn.flipphotowidget.SharedData.Companion.Layouts
import com.pmmn.flipphotowidget.SharedData.Companion.appContext
import com.pmmn.flipphotowidget.SharedData.Companion.appWidgetManager
import com.pmmn.flipphotowidget.SharedData.Companion.await
import com.pmmn.flipphotowidget.SharedData.Companion.imageFutures
import com.pmmn.flipphotowidget.SharedData.Companion.loadImageView
import com.pmmn.flipphotowidget.SharedData.Companion.remoteViews
import com.pmmn.flipphotowidget.SharedData.Companion.updateRemoteImages
import com.pmmn.flipphotowidget.SharedData.Companion.updateRemoteLayout
import com.pmmn.flipphotowidget.SharedData.Companion.uri_iterators
import com.pmmn.flipphotowidget.SharedData.Companion.widgetComponentName
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    // creating variables on below line.
    private lateinit var btnCycle: Button

    private lateinit var previewView: View
    var tiles: List<MutableList<LinearLayout>> = listOf(mutableListOf(), mutableListOf())
    var images: List<MutableList<ImageView>> = listOf(mutableListOf(), mutableListOf())

    private var selectedTileDir = 0
    private var selectedTile = 0
    private val layoutOrder = Layouts.entries.map { it.id }
    private val layoutIterator = ResettableIterator(layoutOrder)

    private fun addImageClickListener() {
        imageIds.forEachIndexed { i, imageIdDir ->
            images[i].clear()
            imageIdDir.forEach { imageId ->
                images[i].add(findViewById(imageId))
            }
        }

        // adding click listener for button on below line.
        images.forEachIndexed { i, imageDir ->
            imageDir.forEachIndexed { j, image ->
                image.setOnClickListener {
                    selectedTileDir = i
                    selectedTile = j
                    pickMultipleMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            }
        }
    }

    private fun applyViews() {
        for (appWidgetId in appWidgetManager.getAppWidgetIds(widgetComponentName)) {
            // Update the widget using AppWidgetManager
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)

            // Inflate the RemoteViews into a standard View
            val remoteViewContainer = findViewById<LinearLayout>(R.id.idPreviewContainer)
            previewView = remoteViews.apply(appContext, remoteViewContainer)

            // Add the inflated RemoteViews to the container
            remoteViewContainer.removeAllViews()
            remoteViewContainer.addView(previewView)

            addImageClickListener()
        }
    }

    // Registers a photo picker activity launcher in multi-select mode.
    private val pickMultipleMedia: ActivityResultLauncher<PickVisualMediaRequest> =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            // Callback is invoked after the user selects media items or closes the
            // photo picker.
            // Take persistable URI permission for each selected URI
            uris.forEach { uri ->
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    e.printStackTrace()  // Log or handle permission error
                }
            }

            if (uris.isNotEmpty()) {
                Log.d("PhotoPicker", "Number of items selected: ${uris.size}")
                tiles_uris[selectedTile].clear()
                tiles_uris[selectedTile].addAll(uris)
                uri_iterators[selectedTile] = ResettableIterator(tiles_uris[selectedTile])


                // Save data in SharedPreferences
                val sharedPreferences = getSharedPreferences("Config", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                // Loop through each MutableList<Uri> and store it as a Set<String>
                tiles_uris.forEachIndexed { index, tile_uris ->
                    val uriStrings = tile_uris.map { it.toString() }.toSet() // Convert Uri to String
                    editor.putStringSet("uri_set_$index", uriStrings)  // Store the set
                }
                val indices_set = uri_iterators.map { it.getCurrentIndex().toString() }.toSet()
                editor.putStringSet("indices_set", indices_set)

                editor.apply()

                // Update the RemoteViews and apply changes
                loadImageView(selectedTile, imageIds[selectedTileDir][selectedTile])
                updateAndApplyViews()
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }

    private fun updateAndApplyViews() {
        // Needed coroutine in a non-blocking way, allowing Glide to run on the main thread
        CoroutineScope(Dispatchers.Main).launch {
            try {
                imageFutures.forEach { future ->
                    future.await()  // Non-blocking wait for the future to complete
                }
                applyViews()
            } catch (e: Exception) {
                println("Failed: ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find the container where RemoteViews will be displayed
        val remoteViewContainer = findViewById<LinearLayout>(R.id.idPreviewContainer)


        // Inflate the RemoteViews into a standard View
        previewView = remoteViews.apply(appContext, remoteViewContainer)

        // Add the inflated RemoteViews to the container
        remoteViewContainer.addView(previewView)

        tileIds.forEachIndexed { i, tileIdDir ->
            tileIdDir.forEach { tileId ->
                tiles[i].add(findViewById(tileId))
            }
        }
        imageIds.forEachIndexed { i, imageIdDir ->
            images[i].clear()
            imageIdDir.forEach { imageId ->
                images[i].add(findViewById(imageId))
            }
        }

        // Save data in SharedPreferences
        val sharedPreferences = getSharedPreferences("Config", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("layout", layout)
        editor.apply()

        btnCycle = findViewById(R.id.idBtnCycle)
        btnCycle.setOnClickListener {
            // Update the layout
            layout = layoutIterator.next()

            // Save data in SharedPreferences
            editor.putInt("layout", layout)
            editor.apply()

            // Update the RemoteViews and apply changes
            updateRemoteLayout()
            updateRemoteImages(sharedPreferences.getStringSet("indices_set", emptySet())?.map { it.toInt() })
            updateAndApplyViews()
        }

        addImageClickListener()

    }

}
