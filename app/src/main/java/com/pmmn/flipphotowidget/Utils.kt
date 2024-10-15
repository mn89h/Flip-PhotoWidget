package com.pmmn.flipphotowidget

import android.content.Context
import android.util.DisplayMetrics
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.InputStream

class Utils {
    companion object {
        // Function to convert dp to pixels
        fun dpToPx(context: Context, dp: Int): Int {
            val displayMetrics: DisplayMetrics = context.resources.displayMetrics
            return (dp * (displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)).toInt()
        }

        fun getBitmapFromUri(contentResolver: ContentResolver, uri: Uri, maxWidth: Int, maxHeight: Int)
        : Bitmap? {
            val options = BitmapFactory.Options()

            // Step 1: Decode the image dimensions without loading it into memory
            options.inJustDecodeBounds = true
            var inputStream: InputStream? = null
            try {
                inputStream = contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            } finally {
                inputStream?.close()
            }

            // Step 2: Calculate the sample size based on required width and height
            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)

            // Step 3: Decode the bitmap with the calculated sample size
            options.inJustDecodeBounds = false
            return try {
                inputStream = contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(inputStream, null, options)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                inputStream?.close()
            }
        }

        // Helper function to calculate inSampleSize
        fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int)
        : Int {
            // Raw height and width of the image
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {
                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                val halfHeight = height / 2
                val halfWidth = width / 2

                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }
    }
}