package com.mmdparsadev

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.mmdparsadev.data.preferences.AppPreferences
import com.mmdparsadev.data.repository.ConversionRepository
import com.mmdparsadev.engine.ConversionEngine
import com.mmdparsadev.engine.plugins.ConversionResult
import com.mmdparsadev.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

data class ConvertedFileInfo(
    val originalName: String,
    val convertedName: String,
    val format: String,
    val size: Long,
    val tempFile: File,
    val mimeType: String
)

class ShareConvertActivity : ComponentActivity() {

    private lateinit var repository: ConversionRepository

    override fun attachBaseContext(newBase: Context) {
        val prefs = AppPreferences(newBase)
        val language = prefs.appLanguage
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = ConversionRepository(applicationContext)

        val intent = intent
        val action = intent?.action
        val type = intent?.type

        val uris = mutableListOf<Uri>()

        if (Intent.ACTION_SEND == action && type != null) {
            val uri = androidx.core.content.IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            if (uri != null) {
                uris.add(uri)
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action && type != null) {
            val list = androidx.core.content.IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            if (list != null) {
                uris.addAll(list)
            }
        }

        if (uris.isEmpty()) {
            Toast.makeText(this, "No files received for conversion.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            MyApplicationTheme {
                ShareConvertPopupScreen(
                    uris = uris,
                    onDismiss = { finish() },
                    onConvert = { targetFormat, onProgress, onComplete, onError ->
                        lifecycleScope.launch {
                            performConversions(uris, targetFormat, onProgress, onComplete, onError)
                        }
                    }
                )
            }
        }
    }

    private suspend fun performConversions(
        uris: List<Uri>,
        targetFormat: String,
        onProgress: (Float) -> Unit,
        onComplete: (List<ConvertedFileInfo>) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val totalFiles = uris.size
                val convertedList = mutableListOf<ConvertedFileInfo>()

                for ((index, uri) in uris.withIndex()) {
                    val fileInfo = getFileInfo(applicationContext, uri)
                    val mimeType = getMimeType(applicationContext, uri)
                    val extension = getExtension(fileInfo.first, mimeType)

                    val inputName = fileInfo.first
                    val cleanInputName = if (inputName.contains(".")) inputName.substringBeforeLast(".") else inputName

                    val progressMultiplier = 1f / totalFiles
                    val baseProgress = index * progressMultiplier

                    // 1. Create a local temporary file in cache directory
                    val tempOutputFile = File(applicationContext.cacheDir, "temp_converted_${System.currentTimeMillis()}_$index.$targetFormat")

                    val result = repository.executeConversion(
                        inputName = inputName,
                        inputType = extension,
                        outputType = targetFormat,
                        inputProvider = { contentResolver.openInputStream(uri) },
                        outputProvider = { FileOutputStream(tempOutputFile) },
                        onProgress = { fileProgress ->
                            val overallProgress = baseProgress + (fileProgress / 100f) * progressMultiplier
                            onProgress(overallProgress)
                        },
                        isCancelled = { false }
                    )

                    if (result is ConversionResult.Success) {
                        // 2. Validate output file
                        if (validateOutputFile(tempOutputFile, targetFormat)) {
                            val outMime = getMimeTypeFromExtension(targetFormat)
                            val finalTargetName = "$cleanInputName.$targetFormat"
                            convertedList.add(
                                ConvertedFileInfo(
                                    originalName = inputName,
                                    convertedName = finalTargetName,
                                    format = targetFormat,
                                    size = tempOutputFile.length(),
                                    tempFile = tempOutputFile,
                                    mimeType = outMime
                                )
                            )
                        } else {
                            tempOutputFile.delete()
                            withContext(Dispatchers.Main) {
                                onError("Converted file is corrupted or empty. Validation failed.")
                            }
                            return@withContext
                        }
                    } else if (result is ConversionResult.Error) {
                        tempOutputFile.delete()
                        withContext(Dispatchers.Main) {
                            onError(result.message)
                        }
                        return@withContext
                    }
                }

                if (convertedList.size == totalFiles) {
                    withContext(Dispatchers.Main) {
                        onComplete(convertedList)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError("Some conversions failed.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Conversion failed.")
                }
            }
        }
    }

    private fun validateOutputFile(file: File, format: String): Boolean {
        if (!file.exists()) return false
        val size = file.length()
        if (size <= 0) return false

        val lFormat = format.lowercase().trim()
        if (lFormat == "png" || lFormat == "jpg" || lFormat == "jpeg" || lFormat == "webp") {
            return try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, options)
                options.outWidth > 0 && options.outHeight > 0
            } catch (e: Exception) {
                false
            }
        }

        if (lFormat == "pdf") {
            return try {
                file.inputStream().use { input ->
                    val header = ByteArray(4)
                    val read = input.read(header)
                    read == 4 && header[0] == '%'.code.toByte() && header[1] == 'P'.code.toByte() && header[2] == 'D'.code.toByte() && header[3] == 'F'.code.toByte()
                }
            } catch (e: Exception) {
                false
            }
        }

        return true
    }

    private fun getFileInfo(context: Context, uri: Uri): Pair<String, Long> {
        var name = "shared_file"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(name, size)
    }

    private fun getMimeType(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri) ?: "*/*"
    }

    private fun getExtension(fileName: String, mimeType: String): String {
        if (fileName.contains(".")) {
            return fileName.substringAfterLast(".").lowercase()
        }
        if (mimeType.contains("/")) {
            val subtype = mimeType.substringAfter("/")
            if (subtype == "jpeg") return "jpg"
            return subtype
        }
        return ""
    }

    private fun getMimeTypeFromExtension(ext: String): String {
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
        if (mime != null) return mime
        return when (ext.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "pdf" -> "application/pdf"
            "mp3" -> "audio/mpeg"
            "mp4" -> "video/mp4"
            "txt" -> "text/plain"
            "md" -> "text/markdown"
            else -> "*/*"
        }
    }
}

// Package-level helper methods for Open, Share, Save
fun saveConvertedFileToDownloads(context: Context, info: ConvertedFileInfo): Uri? {
    val targetName = info.convertedName
    val mimeType = info.mimeType
    
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, targetName)
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    info.tempFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                contentValues.clear()
                contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                return uri
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val file = File(downloadsDir, targetName)
            file.outputStream().use { out ->
                info.tempFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf(mimeType),
                null
            )
            val fileUri = Uri.fromFile(file)
            return fileUri
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

fun shareConvertedFiles(context: Context, files: List<ConvertedFileInfo>) {
    if (files.isEmpty()) return
    
    val uris = ArrayList<Uri>()
    for (info in files) {
        try {
            val fileProviderUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                info.tempFile
            )
            uris.add(fileProviderUri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    if (uris.isEmpty()) return
    
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = files.first().mimeType
            putExtra(Intent.EXTRA_STREAM, uris.first())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_title)))
}

fun openConvertedFile(context: Context, info: ConvertedFileInfo) {
    try {
        val fileProviderUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            info.tempFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileProviderUri, info.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.no_app_found), Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareConvertPopupScreen(
    uris: List<Uri>,
    onDismiss: () -> Unit,
    onConvert: (String, (Float) -> Unit, (List<ConvertedFileInfo>) -> Unit, (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var isConverting by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var convertedFiles by remember { mutableStateOf<List<ConvertedFileInfo>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaved by remember { mutableStateOf(false) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            var allSaved = true
            for (f in convertedFiles ?: emptyList()) {
                val uri = saveConvertedFileToDownloads(context, f)
                if (uri == null) {
                    allSaved = false
                }
            }
            if (allSaved) {
                isSaved = true
                Toast.makeText(context, context.getString(R.string.share_done_saved), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, context.getString(R.string.share_save_error), Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Storage permission is required to save files.", Toast.LENGTH_LONG).show()
        }
    }

    val fileInfos = remember(uris) {
        uris.map { uri ->
            var name = "shared_file"
            var size = 0L
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) name = cursor.getString(nameIdx) ?: name
                        if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val mime = context.contentResolver.getType(uri) ?: "*/*"
            var ext = if (name.contains(".")) name.substringAfterLast(".").lowercase() else ""
            if (ext.isEmpty() && mime.contains("/")) {
                val sub = mime.substringAfter("/")
                ext = if (sub == "jpeg") "jpg" else sub
            }
            Triple(name, size, ext)
        }
    }

    val isMultiple = fileInfos.size > 1
    val mainFile = fileInfos.first()
    val mainExt = mainFile.third

    val suggestions = remember(mainExt) {
        val formats = ConversionEngine.getSuggestedFormats(mainExt)
        if (formats.isEmpty()) {
            listOf("pdf", "zip", "txt")
        } else {
            formats
        }
    }

    var chosenFormat by remember { mutableStateOf(suggestions.firstOrNull()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(enabled = !isConverting) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable(enabled = false) {} // block clicks on dialog body
                .systemBarsPadding(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                if (convertedFiles != null) {
                    // Success View / Result Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.conversion_success),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            convertedFiles!!.forEach { fileInfo ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when (fileInfo.format.lowercase()) {
                                                "png", "jpg", "jpeg", "webp" -> Icons.Default.Image
                                                "mp3", "wav", "aac" -> Icons.Default.AudioFile
                                                "mp4", "gif", "webm" -> Icons.Default.VideoFile
                                                "pdf" -> Icons.Default.PictureAsPdf
                                                else -> Icons.AutoMirrored.Filled.InsertDriveFile
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = fileInfo.convertedName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 32.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = stringResource(R.string.output_format_label) + ": " + fileInfo.format.uppercase(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            val sizeStr = remember(fileInfo.size) {
                                                val size = fileInfo.size
                                                if (size <= 0) "Unknown"
                                                else if (size < 1024 * 1024) String.format("%.1f KB", size / 1024.0)
                                                else String.format("%.1f MB", size / (1024.0 * 1024.0))
                                            }
                                            Text(
                                                text = stringResource(R.string.file_size_label) + ": " + sizeStr,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (isSaved) "Saved" else stringResource(R.string.status_completed),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                                if (convertedFiles!!.size > 1 && fileInfo != convertedFiles!!.last()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { openConvertedFile(context, convertedFiles!!.first()) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.action_open), fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { shareConvertedFiles(context, convertedFiles!!) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.action_share), fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                                        androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                
                                if (needsPermission) {
                                    permissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                } else {
                                    var allSaved = true
                                    for (f in convertedFiles!!) {
                                        val uri = saveConvertedFileToDownloads(context, f)
                                        if (uri == null) {
                                            allSaved = false
                                        }
                                    }
                                    if (allSaved) {
                                        isSaved = true
                                        Toast.makeText(context, context.getString(R.string.share_done_saved), Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.share_save_error), Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.2f),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.action_save), fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(text = "Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                } else {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isMultiple) Icons.AutoMirrored.Filled.LibraryBooks else Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isMultiple) stringResource(R.string.share_multiple_files_title) else stringResource(R.string.share_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // File Details
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isMultiple) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.share_file_name_label),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.share_multiple_files_desc, fileInfos.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.share_file_name_label),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = mainFile.first,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .padding(start = 16.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.share_file_type_label),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val sizeStr = remember(mainFile.second) {
                                        val size = mainFile.second
                                        if (size <= 0) "Unknown"
                                        else if (size < 1024 * 1024) String.format("%.1f KB", size / 1024.0)
                                        else String.format("%.1f MB", size / (1024.0 * 1024.0))
                                    }
                                    Text(
                                        text = "${mainFile.third.uppercase()} ($sizeStr)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.share_current_format_label),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = mainExt.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (!isConverting && errorMessage == null) {
                        // Selection view
                        Text(
                            text = stringResource(R.string.share_convert_to_label),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(suggestions) { format ->
                                val isSelected = chosenFormat == format
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { chosenFormat = format },
                                    label = { Text(format.uppercase()) },
                                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { onDismiss() },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            ) {
                                Text(text = stringResource(R.string.share_cancel))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val target = chosenFormat
                                    if (target != null) {
                                        isConverting = true
                                        onConvert(
                                            target,
                                            { currentProgress -> progress = currentProgress },
                                            { results ->
                                                isConverting = false
                                                convertedFiles = results
                                            },
                                            { error ->
                                                isConverting = false
                                                errorMessage = error
                                            }
                                        )
                                    }
                                },
                                enabled = chosenFormat != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(text = stringResource(R.string.share_start_conversion))
                            }
                        }
                    } else {
                        // Progress or error view
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (isConverting) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.share_converting),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format("%.0f%%", progress * 100),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (errorMessage != null) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        errorMessage = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text(text = "Retry", color = MaterialTheme.colorScheme.onError)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
