package com.mmdparsadev.data.repository

import android.content.Context
import com.mmdparsadev.data.database.AppDatabase
import com.mmdparsadev.data.database.HistoryEntity
import com.mmdparsadev.data.preferences.AppPreferences
import com.mmdparsadev.engine.ConversionEngine
import com.mmdparsadev.engine.plugins.ConversionResult
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream

class ConversionRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.historyDao()
    val preferences = AppPreferences(context)

    val history: Flow<List<HistoryEntity>> = dao.getAllHistory()

    suspend fun addHistory(
        inputName: String,
        inputType: String,
        outputName: String,
        outputType: String,
        isSuccess: Boolean,
        details: String = ""
    ): Long {
        val entity = HistoryEntity(
            inputName = inputName,
            inputType = inputType,
            outputName = outputName,
            outputType = outputType,
            isSuccess = isSuccess,
            details = details
        )
        return dao.insertHistory(entity)
    }

    suspend fun deleteHistory(id: Long) {
        dao.deleteHistoryById(id)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    suspend fun executeConversion(
        inputName: String,
        inputType: String,
        outputType: String,
        inputProvider: () -> InputStream?,
        outputProvider: () -> OutputStream?,
        onProgress: (Float) -> Unit,
        isCancelled: () -> Boolean
    ): ConversionResult {
        val plugin = ConversionEngine.findPluginFor(inputType, outputType)
            ?: return ConversionResult.Error(Exception("No plugin found"), "No conversion engine available for $inputType to $outputType")

        val outputName = if (inputName.contains(".")) {
            inputName.substringBeforeLast(".") + "." + outputType
        } else {
            "$inputName.$outputType"
        }

        val result = plugin.convert(
            context = context,
            inputName = inputName,
            inputType = inputType,
            outputType = outputType,
            inputProvider = inputProvider,
            outputProvider = outputProvider,
            onProgress = onProgress,
            isCancelled = isCancelled
        )

        // Save history in the local Room DB
        when (result) {
            is ConversionResult.Success -> {
                addHistory(inputName, inputType, outputName, outputType, true, result.details)
            }
            is ConversionResult.Error -> {
                addHistory(inputName, inputType, outputName, outputType, false, result.message)
            }
        }

        return result
    }
}
