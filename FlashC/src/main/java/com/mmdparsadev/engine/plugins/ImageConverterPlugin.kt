package com.mmdparsadev.engine.plugins

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import java.io.InputStream
import java.io.OutputStream

class ImageConverterPlugin : ConversionPlugin {
    override val metadata = PluginMetadata(
        id = "image_converter",
        name = "Image Converter",
        description = "Converts images between PNG, JPG, WEBP and creates PDF documents.",
        category = "Image",
        supportedInputTypes = listOf("png", "jpg", "jpeg", "webp", "image/png", "image/jpeg", "image/webp"),
        supportedOutputTypes = listOf("jpg", "png", "webp", "pdf")
    )

    override suspend fun convert(
        context: Context,
        inputName: String,
        inputType: String,
        outputType: String,
        inputProvider: () -> InputStream?,
        outputProvider: () -> OutputStream?,
        onProgress: (Float) -> Unit,
        isCancelled: () -> Boolean
    ): ConversionResult {
        return try {
            onProgress(10f)
            if (isCancelled()) return ConversionResult.Error(Exception("Cancelled"), "Conversion cancelled by user")

            val inputStream = inputProvider() ?: return ConversionResult.Error(Exception("Input missing"), "Could not open source file")
            
            // Avoid loading huge bitmaps completely into RAM if possible, but let's parse with sample size if large
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            // First read dimensions
            val tempStream = inputProvider()
            if (tempStream != null) {
                BitmapFactory.decodeStream(tempStream, null, options)
                tempStream.close()
            }

            if (isCancelled()) return ConversionResult.Error(Exception("Cancelled"), "Conversion cancelled by user")

            onProgress(30f)
            val decodeOptions = BitmapFactory.Options().apply {
                // Scale down if exceeds 4096px
                inSampleSize = 1
                if (options.outWidth > 4096 || options.outHeight > 4096) {
                    inSampleSize = 2
                }
            }
            
            val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream.close()

            if (bitmap == null) {
                return ConversionResult.Error(Exception("Decode failed"), "Unsupported or corrupted image file")
            }

            onProgress(60f)
            if (isCancelled()) {
                bitmap.recycle()
                return ConversionResult.Error(Exception("Cancelled"), "Conversion cancelled by user")
            }

            val outputStream = outputProvider() ?: run {
                bitmap.recycle()
                return ConversionResult.Error(Exception("Output missing"), "Could not open destination stream")
            }

            val format = outputType.lowercase().trim()
            val result = when (format) {
                "pdf" -> {
                    // Draw bitmap onto PDF page
                    val pdfDocument = PdfDocument()
                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(page)
                    
                    if (isCancelled()) {
                        pdfDocument.close()
                        bitmap.recycle()
                        outputStream.close()
                        return ConversionResult.Error(Exception("Cancelled"), "Conversion cancelled by user")
                    }
                    
                    pdfDocument.writeTo(outputStream)
                    pdfDocument.close()
                    ConversionResult.Success(
                        message = "Successfully created PDF document from image",
                        details = "${bitmap.width}x${bitmap.height} px drawing rendered to PDF canvas."
                    )
                }
                "png" -> {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    ConversionResult.Success("Saved as PNG image", "${bitmap.width}x${bitmap.height} px lossless.")
                }
                "webp" -> {
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 85, outputStream)
                    ConversionResult.Success("Converted to WEBP image", "${bitmap.width}x${bitmap.height} px compressed.")
                }
                else -> { // Default is JPEG
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    ConversionResult.Success("Converted to JPG image", "${bitmap.width}x${bitmap.height} px compressed.")
                }
            }

            outputStream.flush()
            outputStream.close()
            bitmap.recycle()
            onProgress(100f)
            result
        } catch (e: Exception) {
            ConversionResult.Error(e, "Image conversion error: ${e.localizedMessage}")
        }
    }
}
