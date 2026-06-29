package com.mmdparsadev

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.mmdparsadev.data.preferences.AppPreferences
import com.mmdparsadev.data.repository.ConversionRepository
import com.mmdparsadev.ui.screens.MainScreen
import com.mmdparsadev.ui.screens.MainViewModel
import com.mmdparsadev.ui.screens.MainViewModelFactory
import com.mmdparsadev.ui.theme.MyApplicationTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

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
        android.util.Log.d("FlashC", "MainActivity.onCreate() - Hash: ${this.hashCode()}, Thread: ${Thread.currentThread().name}, Lifecycle: ${lifecycle.currentState}")
        enableEdgeToEdge()

        val repository = ConversionRepository(applicationContext)
        val factory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
        android.util.Log.d("FlashC", "ViewModel initialized - Hash: ${viewModel.hashCode()}")

        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
        
        // Handle intent after setContent
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        android.util.Log.d("FlashC", "MainActivity.onNewIntent() - Hash: ${this.hashCode()}, Thread: ${Thread.currentThread().name}, Lifecycle: ${lifecycle.currentState}")
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        android.util.Log.d("FlashC", "handleIncomingIntent() called with intent: $intent - Hash: ${this.hashCode()}")
        if (intent == null) return
        val handled = intent.getBooleanExtra("handled_by_flashc", false)
        if (!handled) {
            intent.putExtra("handled_by_flashc", true)
            android.util.Log.d("FlashC", "Calling viewModel.processIntent()")
            viewModel.processIntent(applicationContext, intent)
        } else {
            android.util.Log.d("FlashC", "Intent already handled_by_flashc")
        }
    }
}
