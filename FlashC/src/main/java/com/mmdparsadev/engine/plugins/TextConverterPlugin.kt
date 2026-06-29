package com.mmdparsadev.engine.plugins

import android.content.Context
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.InputStream
import java.io.OutputStream

class TextConverterPlugin : ConversionPlugin {
    override val metadata = PluginMetadata(
        id = "text_converter",
        name = "Text & Document Converter",
        description = "Converts plain text files to professional multi-page PDF documents or Markdown files.",
        category = "Text",
        supportedInputTypes = listOf("text/plain", "txt", "text/*", "md", "csv"),
        supportedOutputTypes = listOf("pdf", "md", "txt")
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
            val text = inputProvider()?.bufferedReader()?.use { it.readText() } ?: ""
            onProgress(40f)

            if (isCancelled()) return ConversionResult.Error(Exception("Cancelled"), "Conversion cancelled by user")

            val outputStream = outputProvider() ?: return ConversionResult.Error(Exception("Output missing"), "Could not open destination stream")
            val format = outputType.lowercase().trim()

            val result = when (format) {
                "pdf" -> {
                    val pdfDocument = PdfDocument()
                    val pageWidth = 595 // A4 standard width in PostScript points
                    val pageHeight = 842 // A4 standard height
                    
                    val textPaint = TextPaint().apply {
                        color = Color.BLACK
                        textSize = 12f
                        isAntiAlias = true
                    }

                    val margin = 50
                    val contentWidth = pageWidth - (margin * 2)
                    
                    // Generate wrapped paragraphs
                    val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, contentWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1.3f)
                        .setIncludePad(false)
                        .build()

                    onProgress(70f)
                    var pageNumber = 1
                    var currentY = margin
                    
                    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    var page = pdfDocument.startPage(pageInfo)
                    var canvas = page.canvas

                    for (lineIndex in 0 until staticLayout.lineCount) {
                        if (isCancelled()) {
                            pdfDocument.close()
                            outputStream.close()
                            return ConversionResult.Error(Exception("Cancelled"), "Conversion cancelled by user")
                        }

                        val lineStart = staticLayout.getLineStart(lineIndex)
                        val lineEnd = staticLayout.getLineEnd(lineIndex)
                        val lineText = text.substring(lineStart, lineEnd).replace("\n", "")

                        canvas.drawText(lineText, margin.toFloat(), currentY.toFloat() + 10f, textPaint)
                        currentY += (textPaint.textSize * 1.5).toInt()

                        // Page break logic
                        if (currentY > pageHeight - margin && lineIndex < staticLayout.lineCount - 1) {
                            pdfDocument.finishPage(page)
                            pageNumber++
                            currentY = margin
                            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                            page = pdfDocument.startPage(pageInfo)
                            canvas = page.canvas
                        }
                    }

                    pdfDocument.finishPage(page)
                    pdfDocument.writeTo(outputStream)
                    pdfDocument.close()
                    ConversionResult.Success("Generated PDF document", "Successfully rendered $pageNumber margins-aligned pages.")
                }
                "md" -> {
                    val mdText = buildString {
                        append("# ${inputName.substringBeforeLast(".")}\n\n")
                        append("> Automatically generated via FlashC\n\n")
                        append(text)
                    }
                    outputStream.write(mdText.toByteArray())
                    ConversionResult.Success("Converted to Markdown", "Styled document with Markdown headings.")
                }
                else -> {
                    outputStream.write(text.toByteArray())
                    ConversionResult.Success("Saved as Plain Text", "Saved raw plain text segment.")
                }
            }

            outputStream.flush()
            outputStream.close()
            onProgress(100f)
            result
        } catch (e: Exception) {
            ConversionResult.Error(e, "Text conversion failed: ${e.localizedMessage}")
        }
    }
}
